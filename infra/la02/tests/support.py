"""Shared test fixtures. Secrets here are fake and never used in production."""
from __future__ import print_function

import copy
import datetime
import sys
from pathlib import Path

LIBEXEC = Path(__file__).resolve().parents[1] / "libexec"
if str(LIBEXEC) not in sys.path:
    sys.path.insert(0, str(LIBEXEC))

UTC = datetime.timezone.utc

FAKE_PRIVATE = "0" * 43 + "="
FAKE_PUBLIC = "1" * 43 + "="
FAKE_SHORT_ID = "0123456789abcdef"

NODE_SECRETS = {
    "schema_version": 1,
    "node_id": "LA02",
    "server_ipv4": "45.63.95.89",
    "node_fqdn": "la02.k8izuh.com",
    "reality": {
        "private_key": FAKE_PRIVATE,
        "public_key": FAKE_PUBLIC,
        "short_id": FAKE_SHORT_ID,
        "handshake_host": "www.cloudflare.com",
        "handshake_port": 443,
    },
}


def utc_now():
    return datetime.datetime.now(UTC).replace(microsecond=0)


def iso(dt):
    return dt.astimezone(UTC).strftime("%Y-%m-%dT%H:%M:%SZ")


def make_user(user_id, n, enabled=True, expires_at=None, created_at=None):
    if created_at is None:
        created_at = iso(utc_now())
    return {
        "user_id": user_id,
        "enabled": enabled,
        "vless_uuid": "00000000-0000-4000-8000-%012d" % n,
        "hy2_password": "hy2-password-%02d-xxxxxxxxxxxxxxxxxxxxxxxx" % n,
        "created_at": created_at,
        "expires_at": expires_at,
        "note": None,
    }


def make_users(*users):
    return {
        "schema_version": 1,
        "node_id": "LA02",
        "capacity": 10,
        "users": [copy.deepcopy(u) for u in users],
    }
