"""Generate sing-box config.json from node-secrets.json and users.json."""
from __future__ import print_function

import argparse
import datetime
import json
import os
import sys
from collections import Counter

UTC = datetime.timezone.utc
NODE_ID = "LA02"
CAPACITY = 10
DEFAULT_DATA_DIR = "/etc/sing-box/data"


class RenderError(Exception):
    """Invalid node data; do not emit a candidate config."""


def _parse_utc(value):
    if value is None:
        return None
    if not isinstance(value, str) or not value.strip():
        raise RenderError("expires_at must be UTC ISO8601 or null")
    text = value.strip()
    if text.endswith("Z"):
        text = text[:-1] + "+00:00"
    try:
        dt = datetime.datetime.fromisoformat(text)
    except ValueError:
        raise RenderError("expires_at is not valid ISO8601: %s" % value)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=UTC)
    return dt.astimezone(UTC)


def _load_json(path):
    try:
        with open(path, "r", encoding="utf-8") as fh:
            return json.load(fh)
    except OSError as exc:
        raise RenderError("cannot read %s: %s" % (path, exc))
    except json.JSONDecodeError as exc:
        raise RenderError("invalid JSON in %s: %s" % (path, exc))


def _require_str(obj, key, where):
    value = obj.get(key)
    if not isinstance(value, str) or not value.strip():
        raise RenderError("%s.%s is required" % (where, key))
    return value.strip()


def _unique(values, label):
    counts = Counter(values)
    dupes = [item for item, n in counts.items() if n > 1]
    if dupes:
        raise RenderError("duplicate %s" % label)


def live_users(users_doc, now=None):
    if users_doc.get("node_id") != NODE_ID:
        raise RenderError("users.json node_id must be %s" % NODE_ID)
    users = users_doc.get("users")
    if not isinstance(users, list):
        raise RenderError("users.json users must be a list")
    _unique([_require_str(u, "user_id", "user") for u in users], "user_id")
    _unique([_require_str(u, "vless_uuid", "user") for u in users], "vless_uuid")
    _unique([_require_str(u, "hy2_password", "user") for u in users], "hy2_password")

    enabled = [u for u in users if u.get("enabled") is True]
    if len(enabled) > CAPACITY:
        raise RenderError("enabled users exceed capacity %d" % CAPACITY)

    if now is None:
        now = datetime.datetime.now(UTC)
    live = []
    for user in enabled:
        expires = _parse_utc(user.get("expires_at"))
        if expires is not None and expires <= now:
            continue
        live.append(user)
    if not live:
        raise RenderError("need 1-%d live users" % CAPACITY)
    if len(live) > CAPACITY:
        raise RenderError("enabled users exceed capacity %d" % CAPACITY)
    return live


def _reality(secrets):
    if secrets.get("node_id") != NODE_ID:
        raise RenderError("node-secrets.json node_id must be %s" % NODE_ID)
    block = secrets.get("reality")
    if not isinstance(block, dict):
        raise RenderError("node-secrets.json reality is required")
    host = _require_str(block, "handshake_host", "reality")
    port = block.get("handshake_port", 443)
    if port != 443:
        raise RenderError("reality handshake_port must be 443")
    short_id = _require_str(block, "short_id", "reality")
    if len(short_id) != 16:
        raise RenderError("reality short_id must be 16 hex chars")
    fqdn = _require_str(secrets, "node_fqdn", "node-secrets")
    return {
        "private_key": _require_str(block, "private_key", "reality"),
        "short_id": short_id,
        "handshake_host": host,
        "node_fqdn": fqdn,
    }


def render_config(data_dir=DEFAULT_DATA_DIR, now=None):
    data_dir = os.fspath(data_dir)
    secrets = _load_json(os.path.join(data_dir, "node-secrets.json"))
    users_doc = _load_json(os.path.join(data_dir, "users.json"))
    reality = _reality(secrets)
    live = live_users(users_doc, now=now)
    fqdn = reality["node_fqdn"]
    host = reality["handshake_host"]
    return {
        "log": {"level": "info", "timestamp": True},
        "inbounds": [
            {
                "type": "vless",
                "tag": "vless-reality-in",
                "listen": "0.0.0.0",
                "listen_port": 443,
                "users": [
                    {
                        "name": u["user_id"],
                        "uuid": u["vless_uuid"],
                        "flow": "xtls-rprx-vision",
                    }
                    for u in live
                ],
                "tls": {
                    "enabled": True,
                    "server_name": host,
                    "reality": {
                        "enabled": True,
                        "handshake": {"server": host, "server_port": 443},
                        "private_key": reality["private_key"],
                        "short_id": [reality["short_id"]],
                    },
                },
            },
            {
                "type": "hysteria2",
                "tag": "hy2-in",
                "listen": "0.0.0.0",
                "listen_port": 443,
                "users": [
                    {"name": u["user_id"], "password": u["hy2_password"]}
                    for u in live
                ],
                "tls": {
                    "enabled": True,
                    "server_name": fqdn,
                    "certificate_path": "/etc/letsencrypt/live/%s/fullchain.pem" % fqdn,
                    "key_path": "/etc/letsencrypt/live/%s/privkey.pem" % fqdn,
                },
            },
        ],
    }


def write_candidate(config, output_path):
    output_path = os.fspath(output_path)
    payload = json.dumps(config, indent=2, sort_keys=False) + "\n"
    tmp = output_path + ".tmp"
    fd = os.open(tmp, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as fh:
            fh.write(payload)
        os.replace(tmp, output_path)
        os.chmod(output_path, 0o600)
    except Exception:
        if os.path.exists(tmp):
            os.remove(tmp)
        raise


def main(argv=None):
    parser = argparse.ArgumentParser(prog="la02-render-config")
    parser.add_argument(
        "--data-dir",
        default=os.environ.get("LA02_DATA_DIR", DEFAULT_DATA_DIR),
    )
    parser.add_argument("--output", required=True)
    args = parser.parse_args(argv)
    try:
        config = render_config(data_dir=args.data_dir)
        write_candidate(config, args.output)
    except RenderError as exc:
        print("la02-render-config: %s" % exc, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
