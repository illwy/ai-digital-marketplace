"""Client export writes per-user files and must not print secrets."""
import io
import json
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest import mock

from tests.support import NODE_SECRETS, make_user, make_users

import la02.export as export


class ExportTests(unittest.TestCase):
    def setUp(self):
        self.tmp = TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.data = self.root / "data"
        self.out = self.root / "client-info"
        self.data.mkdir()
        (self.data / "node-secrets.json").write_text(
            json.dumps(NODE_SECRETS), encoding="utf-8"
        )
        (self.data / "users.json").write_text(
            json.dumps(make_users(make_user("U001", 1), make_user("U002", 2, enabled=False))),
            encoding="utf-8",
        )

    def tearDown(self):
        self.tmp.cleanup()

    def test_writes_vless_hy2_and_mihomo_for_u001_only(self):
        stdout = io.StringIO()
        with mock.patch("sys.stdout", stdout):
            export.export_clients(data_dir=self.data, output_root=self.out)
        text = stdout.getvalue()
        self.assertNotIn(NODE_SECRETS["reality"]["private_key"], text)
        self.assertNotIn("00000000-0000-4000-8000-000000000001", text)
        self.assertNotIn("hy2-password-01", text)
        user_dir = self.out / "U001"
        vless = (user_dir / "vless.uri").read_text(encoding="utf-8").strip()
        hy2 = (user_dir / "hysteria2.uri").read_text(encoding="utf-8").strip()
        yaml = (user_dir / "mihomo.yaml").read_text(encoding="utf-8")
        self.assertTrue(vless.startswith("vless://"))
        self.assertIn("xtls-rprx-vision", vless)
        self.assertIn("security=reality", vless)
        self.assertIn("sni=www.cloudflare.com", vless)
        self.assertIn("pbk=" + NODE_SECRETS["reality"]["public_key"], vless)
        self.assertIn("sid=0123456789abcdef", vless)
        self.assertNotIn(NODE_SECRETS["reality"]["private_key"], vless)
        self.assertTrue(hy2.startswith("hysteria2://"))
        self.assertIn("sni=la02.k8izuh.com", hy2)
        self.assertIn("insecure=0", hy2)
        self.assertIn("type: vless", yaml)
        self.assertIn("type: hysteria2", yaml)
        self.assertIn("skip-cert-verify: false", yaml)
        self.assertIn("public-key: " + NODE_SECRETS["reality"]["public_key"], yaml)
        self.assertFalse((self.out / "U002").exists())

    def test_missing_public_key_is_rejected(self):
        secrets = json.loads((self.data / "node-secrets.json").read_text(encoding="utf-8"))
        del secrets["reality"]["public_key"]
        (self.data / "node-secrets.json").write_text(json.dumps(secrets), encoding="utf-8")
        with self.assertRaises(export.ExportError):
            export.export_clients(data_dir=self.data, output_root=self.out)


if __name__ == "__main__":
    unittest.main()
