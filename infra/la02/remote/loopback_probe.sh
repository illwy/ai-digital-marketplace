#!/bin/bash
# Protocol loopback probe on LA02. Does not print secrets.
set -eu
WORKDIR=/tmp/la02-loopback
rm -rf "$WORKDIR"
mkdir -m 700 "$WORKDIR"
python3 - <<'PY'
import json, os
wd = "/tmp/la02-loopback"
s = json.load(open("/etc/sing-box/data/node-secrets.json"))
u = json.load(open("/etc/sing-box/data/users.json"))["users"][0]
r = s["reality"]
vless = {
    "log": {"level": "warn"},
    "inbounds": [{"type": "mixed", "listen": "127.0.0.1", "listen_port": 18090}],
    "outbounds": [{
        "type": "vless",
        "server": "127.0.0.1",
        "server_port": 443,
        "uuid": u["vless_uuid"],
        "flow": "xtls-rprx-vision",
        "tls": {
            "enabled": True,
            "server_name": r["handshake_host"],
            "utls": {"enabled": True, "fingerprint": "chrome"},
            "reality": {
                "enabled": True,
                "public_key": r["public_key"],
                "short_id": r["short_id"],
            },
        },
    }],
}
hy2 = {
    "log": {"level": "warn"},
    "inbounds": [{"type": "mixed", "listen": "127.0.0.1", "listen_port": 18091}],
    "outbounds": [{
        "type": "hysteria2",
        "server": "127.0.0.1",
        "server_port": 443,
        "password": u["hy2_password"],
        "tls": {
            "enabled": True,
            "server_name": s["node_fqdn"],
            "insecure": False,
        },
    }],
}
open(os.path.join(wd, "vless.json"), "w").write(json.dumps(vless))
open(os.path.join(wd, "hy2.json"), "w").write(json.dumps(hy2))
os.chmod(os.path.join(wd, "vless.json"), 0o600)
os.chmod(os.path.join(wd, "hy2.json"), 0o600)
PY

probe() {
  local name="$1" cfg="$2" port="$3"
  sing-box run -c "$cfg" >"$WORKDIR/${name}.log" 2>&1 &
  local pid=$!
  sleep 1
  local ip=""
  if ip=$(curl -4fsS --max-time 20 -x "http://127.0.0.1:${port}" https://www.cloudflare.com/cdn-cgi/trace | sed -n 's/^ip=//p'); then
    echo "${name} egress=${ip}"
  else
    echo "${name} curl_fail"
    tail -n 8 "$WORKDIR/${name}.log" | sed 's/uuid[^ ]*//g; s/password[^ ]*//g; s/public_key[^ ]*//g'
  fi
  kill "$pid" 2>/dev/null || true
  wait "$pid" 2>/dev/null || true
}

probe vless "$WORKDIR/vless.json" 18090
probe hy2 "$WORKDIR/hy2.json" 18091
rm -rf "$WORKDIR"
echo LOOPBACK_DONE
