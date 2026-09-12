#!/usr/bin/env python3
"""Probe LA02 VLESS and HY2 via local sing-box client. Never prints secrets."""
from __future__ import print_function

import json
import os
import shutil
import subprocess
import sys
import time
import zipfile
from pathlib import Path

import yaml

SECRETS = Path(os.path.expanduser(r"~\.secrets\la02-U001"))
WORK = Path(os.environ.get("TEMP", "/tmp")) / "la02-client-probe"
SB_VER = "1.13.21"
ZIP_NAME = "sing-box-%s-windows-amd64.zip" % SB_VER
ZIP_URL = (
    "https://github.com/SagerNet/sing-box/releases/download/v%s/%s" % (SB_VER, ZIP_NAME)
)
PROXY = "http://127.0.0.1:10808"
SERVER_IPV4 = os.environ.get("LA02_SERVER_IPV4", "45.63.95.89")
USE_SOCKS = os.environ.get("LA02_USE_SOCKS") == "1"
LOCAL_PORT_VLESS = 17890
LOCAL_PORT_HY2 = 17891


def _curl_get(url, dest):
    cmd = [
        "curl.exe",
        "-fsSL",
        "--max-time",
        "120",
        "-x",
        PROXY,
        "-o",
        str(dest),
        url,
    ]
    subprocess.check_call(cmd)


def ensure_singbox():
    WORK.mkdir(parents=True, exist_ok=True)
    exe = WORK / "sing-box.exe"
    if exe.is_file():
        return exe
    zpath = WORK / ZIP_NAME
    print("DOWNLOAD sing-box", SB_VER)
    _curl_get(ZIP_URL, zpath)
    with zipfile.ZipFile(zpath) as zf:
        for info in zf.infolist():
            if info.filename.endswith("sing-box.exe"):
                with zf.open(info) as src, open(str(exe), "wb") as dst:
                    shutil.copyfileobj(src, dst)
                break
        else:
            raise SystemExit("sing-box.exe not in zip")
    return exe


def load_proxies():
    data = yaml.safe_load((SECRETS / "mihomo.yaml").read_text(encoding="utf-8"))
    found = {}
    for item in data.get("proxies") or []:
        found[item.get("type")] = item
    if "vless" not in found or "hysteria2" not in found:
        raise SystemExit("mihomo.yaml missing vless or hysteria2")
    return found


def vless_cfg(p):
    opts = p.get("reality-opts") or {}
    outbound = {
        "type": "vless",
        "tag": "la02",
        "server": SERVER_IPV4,
        "server_port": int(p["port"]),
        "uuid": p["uuid"],
        "flow": p.get("flow") or "xtls-rprx-vision",
        "tls": {
            "enabled": True,
            "server_name": p.get("servername"),
            "utls": {
                "enabled": True,
                "fingerprint": p.get("client-fingerprint") or "chrome",
            },
            "reality": {
                "enabled": True,
                "public_key": opts.get("public-key"),
                "short_id": opts.get("short-id"),
            },
        },
    }
    outbounds = [outbound]
    if USE_SOCKS:
        outbound["detour"] = "via-socks"
        outbounds.append(
            {
                "type": "socks",
                "tag": "via-socks",
                "server": "127.0.0.1",
                "server_port": 10808,
                "version": "5",
            }
        )
    return {
        "log": {"level": "warn"},
        "inbounds": [
            {
                "type": "mixed",
                "listen": "127.0.0.1",
                "listen_port": LOCAL_PORT_VLESS,
            }
        ],
        "outbounds": outbounds,
    }


def hy2_cfg(p):
    outbound = {
        "type": "hysteria2",
        "tag": "la02",
        "server": SERVER_IPV4,
        "server_port": int(p["port"]),
        "password": p["password"],
        "tls": {
            "enabled": True,
            "server_name": p.get("sni") or p["server"],
            "insecure": bool(p.get("skip-cert-verify")),
        },
    }
    outbounds = [outbound]
    if USE_SOCKS:
        outbound["detour"] = "via-socks"
        outbounds.append(
            {
                "type": "socks",
                "tag": "via-socks",
                "server": "127.0.0.1",
                "server_port": 10808,
                "version": "5",
            }
        )
    return {
        "log": {"level": "warn"},
        "inbounds": [
            {
                "type": "mixed",
                "listen": "127.0.0.1",
                "listen_port": LOCAL_PORT_HY2,
            }
        ],
        "outbounds": outbounds,
    }


def trace_ip(port):
    cmd = [
        "curl.exe",
        "-4fsS",
        "--max-time",
        "25",
        "-x",
        "http://127.0.0.1:%s" % port,
        "https://www.cloudflare.com/cdn-cgi/trace",
    ]
    proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
    if proc.returncode != 0:
        return None, proc.stderr.strip()[-300:]
    ip = None
    for line in proc.stdout.splitlines():
        if line.startswith("ip="):
            ip = line.split("=", 1)[1].strip()
    return ip, None


def run_one(exe, name, cfg, port):
    cfg_path = WORK / ("%s.json" % name)
    cfg_path.write_text(json.dumps(cfg), encoding="utf-8")
    log_path = WORK / ("%s.log" % name)
    proc = subprocess.Popen(
        [str(exe), "run", "-c", str(cfg_path)],
        stdout=open(str(log_path), "w"),
        stderr=subprocess.STDOUT,
    )
    try:
        time.sleep(2)
        if proc.poll() is not None:
            tail = log_path.read_text(encoding="utf-8", errors="replace")[-400:]
            print("%s CLIENT_EXIT %s" % (name, proc.returncode))
            print("%s LOG_TAIL %s" % (name, tail.replace("\n", " | ")))
            return False
        ip, err = trace_ip(port)
        if ip == SERVER_IPV4:
            print("%s PASS egress=%s" % (name, ip))
            return True
        print("%s FAIL egress=%s err=%s" % (name, ip, err))
        return False
    finally:
        if proc.poll() is None:
            proc.terminate()
            try:
                proc.wait(timeout=5)
            except Exception:
                proc.kill()


def main():
    exe = ensure_singbox()
    proxies = load_proxies()
    vless_ok = run_one(exe, "vless", vless_cfg(proxies["vless"]), LOCAL_PORT_VLESS)
    hy2_ok = run_one(exe, "hy2", hy2_cfg(proxies["hysteria2"]), LOCAL_PORT_HY2)
    if vless_ok and hy2_ok:
        print("BOTH_PASS")
        return 0
    if vless_ok:
        print("VLESS_ONLY")
        return 2
    return 1


if __name__ == "__main__":
    sys.exit(main())
