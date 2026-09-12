"""Lightweight LA02 health probe. Exit 0 if healthy, non-zero otherwise."""
from __future__ import print_function

import argparse
import os
import shutil
import subprocess
import sys

DISK_LIMIT = 80.0
CERT_DAYS_MIN = 30


class Probe(object):
    def __init__(self, fqdn="la02.k8izuh.com", config_path="/etc/sing-box/config.json"):
        self.fqdn = fqdn
        self.config_path = config_path

    def _run(self, args):
        return subprocess.run(
            args,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            universal_newlines=True,
        )

    def is_active(self):
        proc = self._run(["systemctl", "is-active", "sing-box"])
        return proc.stdout.strip() == "active"

    def tcp_listening(self):
        proc = self._run(["ss", "-lnt"])
        return ":443" in proc.stdout

    def udp_listening(self):
        proc = self._run(["ss", "-lnu"])
        return ":443" in proc.stdout

    def disk_percent(self):
        usage = shutil.disk_usage("/")
        return (usage.used / float(usage.total)) * 100.0

    def swap_is_growing(self):
        try:
            info = {}
            with open("/proc/meminfo", "r", encoding="utf-8") as fh:
                for line in fh:
                    key, val = line.split(":", 1)
                    info[key] = int(val.strip().split()[0])
            total = info.get("SwapTotal", 0)
            free = info.get("SwapFree", 0)
            return total > 0 and (total - free) > 256 * 1024
        except (OSError, ValueError):
            return False

    def cert_days_remaining(self):
        path = "/etc/letsencrypt/live/%s/fullchain.pem" % self.fqdn
        if not os.path.isfile(path):
            return -1
        proc = self._run(
            ["openssl", "x509", "-in", path, "-noout", "-enddate"]
        )
        # Production timer uses openssl; evaluate() only needs the number.
        # Parsing is done by a small helper to keep this probe honest.
        text = proc.stdout.strip()
        if not text.startswith("notAfter="):
            return -1
        return _days_until(text.split("=", 1)[1])

    def recent_oom(self):
        proc = self._run(
            ["journalctl", "-k", "--since", "24 hours ago"]
        )
        lower = proc.stdout.lower()
        return "oom" in lower or "out of memory" in lower

    def restart_count(self):
        proc = self._run(["systemctl", "show", "sing-box", "-p", "NRestarts"])
        line = proc.stdout.strip()
        if "=" not in line:
            return 0
        try:
            return int(line.split("=", 1)[1])
        except ValueError:
            return 0

    def config_check(self):
        proc = self._run(["sing-box", "check", "-c", self.config_path])
        return proc.returncode == 0


def _days_until(enddate):
    import datetime

    try:
        expiry = datetime.datetime.strptime(enddate, "%b %d %H:%M:%S %Y %Z")
    except ValueError:
        return -1
    now = datetime.datetime.utcnow()
    return (expiry - now).days


def evaluate(probe):
    problems = []
    if not probe.is_active():
        problems.append("sing-box is not active")
    if not probe.tcp_listening():
        problems.append("443/tcp is not listening")
    if not probe.udp_listening():
        problems.append("443/udp is not listening")
    if probe.disk_percent() >= DISK_LIMIT:
        problems.append("disk usage >= 80%")
    if probe.swap_is_growing():
        problems.append("swap is growing")
    if probe.cert_days_remaining() <= CERT_DAYS_MIN:
        problems.append("certificate remaining days <= 30")
    if probe.recent_oom():
        problems.append("recent OOM")
    if not probe.config_check():
        problems.append("sing-box check failed")
    return (1 if problems else 0, problems)


def main(argv=None):
    parser = argparse.ArgumentParser(prog="la02-health")
    parser.add_argument("--fqdn", default=os.environ.get("LA02_FQDN", "la02.k8izuh.com"))
    parser.add_argument(
        "--config",
        default=os.environ.get("LA02_CONFIG", "/etc/sing-box/config.json"),
    )
    args = parser.parse_args(argv)
    code, problems = evaluate(Probe(fqdn=args.fqdn, config_path=args.config))
    for item in problems:
        print("la02-health: %s" % item, file=sys.stderr)
    return code


if __name__ == "__main__":
    sys.exit(main())
