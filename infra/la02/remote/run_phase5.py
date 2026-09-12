#!/usr/bin/env python3
"""Upload CF credentials without printing them, then run Phase 5 certs."""
from __future__ import print_function

import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "libexec"))
from la02.socks_ssh import connect_la02, run

TOKEN_PATH = os.path.expanduser(r"C:\Users\y\.secrets\cf-k8izuh.token")
KEY = os.path.expanduser(r"~\.ssh\la02_ed25519")
SCRIPT = os.path.join(os.path.dirname(__file__), "phase5-certs.sh")


def main():
    token = open(TOKEN_PATH, "r", encoding="utf-8").read().strip()
    if len(token) < 20:
        raise SystemExit("token file looks too short")
    body = "dns_cloudflare_api_token = %s\n" % token
    c = connect_la02(username="deploy", key_filename=KEY)
    sftp = c.open_sftp()
    with sftp.file("/tmp/cloudflare.ini", "w") as fh:
        fh.write(body)
    sftp.put(SCRIPT, "/tmp/phase5-certs.sh")
    sftp.close()
    prep = r"""
set -eu
sudo install -d -m 700 /root/.secrets
sudo install -m 600 /tmp/cloudflare.ini /root/.secrets/cloudflare.ini
sudo rm -f /tmp/cloudflare.ini
sudo chmod 700 /tmp/phase5-certs.sh
sudo stat -c '%a %n' /root/.secrets/cloudflare.ini
"""
    code, out, err = run(c, prep, timeout=30)
    print("PREP", code)
    print(out)
    if err.strip():
        print("PREP_ERR", err[-500:])
    if code != 0:
        c.close()
        return code
    code, out, err = run(c, "sudo ACME_EMAIL=yzrhw0218@163.com LA02_FQDN=la02.k8izuh.com bash /tmp/phase5-certs.sh", timeout=300)
    print("CERTS", code)
    print(out)
    if err.strip():
        # Do not dump full certbot logs if they might contain the ini path only.
        print("CERTS_ERR_TAIL")
        print(err[-2500:])
    c.close()
    return code


if __name__ == "__main__":
    sys.exit(main())
