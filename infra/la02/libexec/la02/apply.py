"""Backup, render, check, atomically install, restart, health-check, roll back."""
from __future__ import print_function

import argparse
import datetime
import hashlib
import os
import shutil
import subprocess
import sys
import time

from la02.render import RenderError, render_config, write_candidate

UTC = datetime.timezone.utc
DEFAULT_CONFIG = "/etc/sing-box/config.json"
DEFAULT_DATA_DIR = "/etc/sing-box/data"
DEFAULT_BACKUP_ROOT = "/var/backups/la02-sing-box"
DEFAULT_LOCK = "/run/lock/la02-config.lock"
DEFAULT_SBIN = "/usr/local/sbin"
KEEP_BACKUPS = 20
KEEP_DAYS = 30
_HELD_LOCKS = set()


class ApplyError(Exception):
    """Apply failed; caller should treat this as a non-zero exit."""


class Paths(object):
    def __init__(
        self,
        config_path=DEFAULT_CONFIG,
        data_dir=DEFAULT_DATA_DIR,
        backup_root=DEFAULT_BACKUP_ROOT,
        lock_path=DEFAULT_LOCK,
        sbin_dir=DEFAULT_SBIN,
        service_group="sing-box",
    ):
        self.config_path = os.fspath(config_path)
        self.data_dir = os.fspath(data_dir)
        self.backup_root = os.fspath(backup_root)
        self.lock_path = os.fspath(lock_path)
        self.sbin_dir = os.fspath(sbin_dir)
        self.service_group = service_group
        self.last_good = os.path.join(self.data_dir, ".last-good")
        config_dir = os.path.dirname(self.config_path).replace("\\", "/")
        if config_dir == "/etc/sing-box":
            self.candidate_path = "/run/la02/config.candidate.json"
        else:
            self.candidate_path = self.config_path + ".candidate"


class SystemHost(object):
    def sing_box_check(self, config_path):
        proc = subprocess.run(
            ["sing-box", "check", "-c", config_path],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            universal_newlines=True,
        )
        if proc.returncode != 0:
            raise ApplyError("sing-box check failed")

    def restart_service(self):
        proc = subprocess.run(
            ["systemctl", "restart", "sing-box"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            universal_newlines=True,
        )
        if proc.returncode != 0:
            raise ApplyError("restart failed")
        time.sleep(1)

    def is_active(self):
        proc = subprocess.run(
            ["systemctl", "is-active", "sing-box"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            universal_newlines=True,
        )
        return proc.stdout.strip() == "active"

    def ports_ok(self):
        tcp = subprocess.run(
            ["ss", "-lnt"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            universal_newlines=True,
        )
        udp = subprocess.run(
            ["ss", "-lnu"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            universal_newlines=True,
        )
        return _has_local_port(tcp.stdout, 443) and _has_local_port(udp.stdout, 443)


def _has_local_port(ss_output, port):
    token = ":%s" % port
    for line in ss_output.splitlines():
        if token in line:
            return True
    return False


def acquire_lock(path):
    path = os.path.abspath(os.fspath(path))
    if path in _HELD_LOCKS:
        raise ApplyError("config lock is held")
    parent = os.path.dirname(path)
    if parent and not os.path.isdir(parent):
        os.makedirs(parent, mode=0o755, exist_ok=True)
    fd = os.open(path, os.O_CREAT | os.O_RDWR, 0o600)
    try:
        if os.path.getsize(path) == 0:
            os.write(fd, b"\0")
            os.lseek(fd, 0, os.SEEK_SET)
        if os.name == "nt":
            import msvcrt

            msvcrt.locking(fd, msvcrt.LK_NBLCK, 1)
        else:
            import fcntl

            fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except (OSError, IOError):
        os.close(fd)
        raise ApplyError("config lock is held")
    _HELD_LOCKS.add(path)
    return fd


def release_lock(fd, path):
    path = os.path.abspath(os.fspath(path))
    try:
        if os.name == "nt":
            import msvcrt

            os.lseek(fd, 0, os.SEEK_SET)
            try:
                msvcrt.locking(fd, msvcrt.LK_UNLCK, 1)
            except (OSError, IOError):
                pass
        else:
            import fcntl

            fcntl.flock(fd, fcntl.LOCK_UN)
    finally:
        os.close(fd)
        _HELD_LOCKS.discard(path)


def _backup(paths):
    ts = datetime.datetime.now(UTC).strftime("%Y%m%dT%H%M%SZ")
    dest = os.path.join(paths.backup_root, ts)
    extra = 0
    while os.path.exists(dest):
        extra += 1
        dest = os.path.join(paths.backup_root, "%s-%02d" % (ts, extra))
    os.makedirs(dest, mode=0o700)
    if os.path.exists(paths.config_path):
        shutil.copy2(paths.config_path, os.path.join(dest, "config.json"))
    data_dest = os.path.join(dest, "data")
    if os.path.isdir(paths.data_dir):
        shutil.copytree(
            paths.data_dir,
            data_dest,
            ignore=shutil.ignore_patterns(".last-good"),
        )
    for name in ("la02-render-config", "la02-apply-config"):
        src = os.path.join(paths.sbin_dir, name)
        if os.path.isfile(src):
            shutil.copy2(src, os.path.join(dest, name))
    _write_checksums(dest)
    _prune_backups(paths.backup_root)
    return dest


def _write_checksums(dest):
    lines = []
    for dirpath, _, filenames in os.walk(dest):
        for name in sorted(filenames):
            if name == "SHA256SUMS":
                continue
            full = os.path.join(dirpath, name)
            digest = hashlib.sha256()
            with open(full, "rb") as fh:
                for chunk in iter(lambda: fh.read(65536), b""):
                    digest.update(chunk)
            rel = os.path.relpath(full, dest).replace("\\", "/")
            lines.append("%s  %s\n" % (digest.hexdigest(), rel))
    with open(os.path.join(dest, "SHA256SUMS"), "w", encoding="utf-8") as fh:
        fh.writelines(lines)


def _prune_backups(backup_root):
    if not os.path.isdir(backup_root):
        return
    entries = []
    for name in os.listdir(backup_root):
        path = os.path.join(backup_root, name)
        if os.path.isdir(path):
            entries.append((os.path.getmtime(path), path))
    entries.sort(reverse=True)
    cutoff = datetime.datetime.now(UTC) - datetime.timedelta(days=KEEP_DAYS)
    cutoff_ts = cutoff.timestamp()
    for index, (mtime, path) in enumerate(entries):
        if index < KEEP_BACKUPS or mtime >= cutoff_ts:
            continue
        shutil.rmtree(path, ignore_errors=True)


def _install_config(src, dest, mode=0o640, group=None):
    tmp = dest + ".tmp"
    shutil.copyfile(src, tmp)
    os.chmod(tmp, mode)
    if group and os.name != "nt":
        try:
            import grp

            gid = grp.getgrnam(group).gr_gid
            os.chown(tmp, 0, gid)
        except (ImportError, KeyError, OSError):
            pass
    os.replace(tmp, dest)


def _snapshot_last_good(paths):
    dest = paths.last_good
    if os.path.isdir(dest):
        shutil.rmtree(dest)
    os.makedirs(dest, mode=0o700)
    if os.path.isfile(paths.config_path):
        shutil.copy2(paths.config_path, os.path.join(dest, "config.json"))
    for name in ("users.json", "node-secrets.json"):
        src = os.path.join(paths.data_dir, name)
        if os.path.isfile(src):
            shutil.copy2(src, os.path.join(dest, name))


def _restore_users_from_last_good(paths):
    dest = paths.last_good
    for name in ("users.json", "node-secrets.json"):
        src = os.path.join(dest, name)
        if os.path.isfile(src):
            shutil.copy2(src, os.path.join(paths.data_dir, name))


def _restore_config(src, paths, host):
    if not os.path.isfile(src):
        return
    _install_config(src, paths.config_path, group=paths.service_group)
    host.sing_box_check(paths.config_path)
    host.restart_service()


def _fail_restore(paths, host, backup_dir, installed):
    _restore_users_from_last_good(paths)
    last_config = os.path.join(paths.last_good, "config.json")
    if os.path.isfile(last_config):
        _restore_config(last_config, paths, host)
        return
    if installed:
        _restore_config(os.path.join(backup_dir, "config.json"), paths, host)


def _health(host):
    if not host.is_active():
        raise ApplyError("sing-box is not active")
    if not host.ports_ok():
        raise ApplyError("443/tcp or 443/udp is not listening")


def apply_config(paths, host=None):
    if host is None:
        host = SystemHost()
    fd = acquire_lock(paths.lock_path)
    installed = False
    backup_dir = None
    try:
        os.makedirs(paths.backup_root, mode=0o700, exist_ok=True)
        cand_dir = os.path.dirname(paths.candidate_path)
        if cand_dir:
            os.makedirs(cand_dir, mode=0o700, exist_ok=True)
        backup_dir = _backup(paths)
        try:
            config = render_config(data_dir=paths.data_dir)
        except RenderError as exc:
            _fail_restore(paths, host, backup_dir, installed)
            raise ApplyError(str(exc))
        write_candidate(config, paths.candidate_path)
        try:
            host.sing_box_check(paths.candidate_path)
        except ApplyError:
            _fail_restore(paths, host, backup_dir, installed)
            raise
        _install_config(
            paths.candidate_path,
            paths.config_path,
            group=paths.service_group,
        )
        installed = True
        host.restart_service()
        try:
            _health(host)
        except ApplyError:
            _fail_restore(paths, host, backup_dir, installed)
            raise
        _snapshot_last_good(paths)
        return 0
    finally:
        if os.path.isfile(paths.candidate_path):
            try:
                os.remove(paths.candidate_path)
            except OSError:
                pass
        release_lock(fd, paths.lock_path)


def main(argv=None):
    parser = argparse.ArgumentParser(prog="la02-apply-config")
    parser.add_argument("--config", default=os.environ.get("LA02_CONFIG", DEFAULT_CONFIG))
    parser.add_argument("--data-dir", default=os.environ.get("LA02_DATA_DIR", DEFAULT_DATA_DIR))
    parser.add_argument(
        "--backup-root",
        default=os.environ.get("LA02_BACKUP_ROOT", DEFAULT_BACKUP_ROOT),
    )
    parser.add_argument("--lock", default=os.environ.get("LA02_LOCK", DEFAULT_LOCK))
    parser.add_argument("--sbin-dir", default=os.environ.get("LA02_SBIN", DEFAULT_SBIN))
    args = parser.parse_args(argv)
    paths = Paths(
        config_path=args.config,
        data_dir=args.data_dir,
        backup_root=args.backup_root,
        lock_path=args.lock,
        sbin_dir=args.sbin_dir,
    )
    try:
        apply_config(paths)
    except ApplyError as exc:
        print("la02-apply-config: %s" % exc, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
