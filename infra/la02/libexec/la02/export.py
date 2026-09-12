"""Write per-user client files. Never print secrets to stdout."""
from __future__ import print_function

import argparse
import os
import sys
from urllib.parse import quote

from la02.render import RenderError, _load_json, _reality, live_users

DEFAULT_DATA_DIR = "/etc/sing-box/data"
DEFAULT_OUTPUT = "/root/la02-client-info"


class ExportError(Exception):
    pass


def _vless_uri(user, fqdn, public_key, short_id, handshake_host):
    name = quote("LA02-VLESS-%s" % user["user_id"], safe="")
    query = "&".join(
        [
            "encryption=none",
            "flow=xtls-rprx-vision",
            "security=reality",
            "sni=%s" % quote(handshake_host, safe=".-"),
            "fp=chrome",
            "pbk=%s" % quote(public_key, safe="=-_"),
            "sid=%s" % quote(short_id, safe=""),
            "type=tcp",
        ]
    )
    return "vless://%s@%s:443?%s#%s" % (user["vless_uuid"], fqdn, query, name)


def _hy2_uri(user, fqdn):
    name = quote("LA02-HY2-%s" % user["user_id"], safe="")
    password = quote(user["hy2_password"], safe="")
    return "hysteria2://%s@%s:443?sni=%s&insecure=0#%s" % (
        password,
        fqdn,
        quote(fqdn, safe=".-"),
        name,
    )


def _mihomo_yaml(user, fqdn, public_key, short_id, handshake_host):
    uid = user["user_id"]
    return "\n".join(
        [
            "proxies:",
            "- name: LA02-VLESS-%s" % uid,
            "  type: vless",
            "  server: %s" % fqdn,
            "  port: 443",
            "  uuid: %s" % user["vless_uuid"],
            "  flow: xtls-rprx-vision",
            "  network: tcp",
            "  udp: true",
            "  tls: true",
            "  servername: %s" % handshake_host,
            "  client-fingerprint: chrome",
            "  reality-opts:",
            "    public-key: %s" % public_key,
            "    short-id: %s" % short_id,
            "- name: LA02-HY2-%s" % uid,
            "  type: hysteria2",
            "  server: %s" % fqdn,
            "  port: 443",
            "  password: %s" % user["hy2_password"],
            "  sni: %s" % fqdn,
            "  skip-cert-verify: false",
            "",
        ]
    )


def export_clients(data_dir=DEFAULT_DATA_DIR, output_root=DEFAULT_OUTPUT):
    try:
        secrets = _load_json(os.path.join(os.fspath(data_dir), "node-secrets.json"))
        users_doc = _load_json(os.path.join(os.fspath(data_dir), "users.json"))
        reality = _reality(secrets)
        live = live_users(users_doc)
    except RenderError as exc:
        raise ExportError(str(exc))
    public_key = (secrets.get("reality") or {}).get("public_key")
    if not isinstance(public_key, str) or not public_key.strip():
        raise ExportError("reality public_key is required for client export")
    public_key = public_key.strip()
    os.makedirs(os.fspath(output_root), mode=0o700, exist_ok=True)
    exported = []
    for user in live:
        user_dir = os.path.join(os.fspath(output_root), user["user_id"])
        os.makedirs(user_dir, mode=0o700, exist_ok=True)
        files = {
            "vless.uri": _vless_uri(
                user,
                reality["node_fqdn"],
                public_key,
                reality["short_id"],
                reality["handshake_host"],
            )
            + "\n",
            "hysteria2.uri": _hy2_uri(user, reality["node_fqdn"]) + "\n",
            "mihomo.yaml": _mihomo_yaml(
                user,
                reality["node_fqdn"],
                public_key,
                reality["short_id"],
                reality["handshake_host"],
            ),
        }
        for name, body in files.items():
            path = os.path.join(user_dir, name)
            fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
            with os.fdopen(fd, "w", encoding="utf-8") as fh:
                fh.write(body)
        exported.append(user["user_id"])
    print("exported %d user(s)" % len(exported))
    return exported


def main(argv=None):
    parser = argparse.ArgumentParser(prog="la02-export-client")
    parser.add_argument("--data-dir", default=os.environ.get("LA02_DATA_DIR", DEFAULT_DATA_DIR))
    parser.add_argument(
        "--output-root",
        default=os.environ.get("LA02_CLIENT_INFO", DEFAULT_OUTPUT),
    )
    args = parser.parse_args(argv)
    try:
        export_clients(data_dir=args.data_dir, output_root=args.output_root)
    except ExportError as exc:
        print("la02-export-client: %s" % exc, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
