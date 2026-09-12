"""Render must emit dual-inbound config from users.json + node-secrets.json."""
import json
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from tests.support import NODE_SECRETS, iso, make_user, make_users, utc_now

import la02.render as render


class RenderTests(unittest.TestCase):
    def setUp(self):
        self.tmp = TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.data = self.root / "data"
        self.data.mkdir()
        self._write_secrets(NODE_SECRETS)
        self._write_users(make_users(make_user("U001", 1)))

    def tearDown(self):
        self.tmp.cleanup()

    def _write_secrets(self, payload):
        (self.data / "node-secrets.json").write_text(
            json.dumps(payload), encoding="utf-8"
        )

    def _write_users(self, payload):
        (self.data / "users.json").write_text(
            json.dumps(payload), encoding="utf-8"
        )

    def _render(self):
        return render.render_config(data_dir=self.data)

    def test_vless_and_hy2_listen_on_443_with_u001(self):
        cfg = self._render()
        self.assertEqual(cfg["log"], {"level": "info", "timestamp": True})
        vless = cfg["inbounds"][0]
        hy2 = cfg["inbounds"][1]
        self.assertEqual(vless["type"], "vless")
        self.assertEqual(vless["tag"], "vless-reality-in")
        self.assertEqual(vless["listen"], "0.0.0.0")
        self.assertEqual(vless["listen_port"], 443)
        self.assertEqual(
            vless["users"],
            [
                {
                    "name": "U001",
                    "uuid": "00000000-0000-4000-8000-000000000001",
                    "flow": "xtls-rprx-vision",
                }
            ],
        )
        self.assertEqual(vless["tls"]["server_name"], "www.cloudflare.com")
        self.assertTrue(vless["tls"]["reality"]["enabled"])
        self.assertEqual(
            vless["tls"]["reality"]["handshake"],
            {"server": "www.cloudflare.com", "server_port": 443},
        )
        self.assertEqual(
            vless["tls"]["reality"]["private_key"], NODE_SECRETS["reality"]["private_key"]
        )
        self.assertEqual(vless["tls"]["reality"]["short_id"], ["0123456789abcdef"])
        self.assertEqual(hy2["type"], "hysteria2")
        self.assertEqual(hy2["tag"], "hy2-in")
        self.assertEqual(hy2["listen_port"], 443)
        self.assertEqual(
            hy2["users"],
            [
                {
                    "name": "U001",
                    "password": "hy2-password-01-xxxxxxxxxxxxxxxxxxxxxxxx",
                }
            ],
        )
        self.assertEqual(hy2["tls"]["server_name"], "la02.k8izuh.com")
        self.assertEqual(
            hy2["tls"]["certificate_path"],
            "/etc/letsencrypt/live/la02.k8izuh.com/fullchain.pem",
        )
        self.assertEqual(
            hy2["tls"]["key_path"],
            "/etc/letsencrypt/live/la02.k8izuh.com/privkey.pem",
        )

    def test_disabled_user_is_omitted_from_both_inbounds(self):
        self._write_users(
            make_users(
                make_user("U001", 1, enabled=True),
                make_user("U002", 2, enabled=False),
            )
        )
        cfg = self._render()
        names_vless = [u["name"] for u in cfg["inbounds"][0]["users"]]
        names_hy2 = [u["name"] for u in cfg["inbounds"][1]["users"]]
        self.assertEqual(names_vless, ["U001"])
        self.assertEqual(names_hy2, ["U001"])

    def test_expired_user_is_omitted_using_utc(self):
        past = iso(utc_now().replace(year=utc_now().year - 1))
        future = iso(utc_now().replace(year=utc_now().year + 1))
        self._write_users(
            make_users(
                make_user("U001", 1, expires_at=None),
                make_user("U002", 2, expires_at=past),
                make_user("U003", 3, expires_at=future),
            )
        )
        cfg = self._render()
        names = [u["name"] for u in cfg["inbounds"][0]["users"]]
        self.assertEqual(names, ["U001", "U003"])

    def test_ten_enabled_users_are_accepted(self):
        users = [make_user("U%03d" % i, i) for i in range(1, 11)]
        users.append(make_user("U011", 11, enabled=False))
        self._write_users(make_users(*users))
        cfg = self._render()
        self.assertEqual(len(cfg["inbounds"][0]["users"]), 10)

    def test_eleventh_enabled_user_is_rejected(self):
        users = [make_user("U%03d" % i, i) for i in range(1, 12)]
        self._write_users(make_users(*users))
        with self.assertRaises(render.RenderError) as ctx:
            self._render()
        self.assertIn("10", str(ctx.exception))

    def test_duplicate_uuid_is_rejected(self):
        u1 = make_user("U001", 1)
        u2 = make_user("U002", 2)
        u2["vless_uuid"] = u1["vless_uuid"]
        self._write_users(make_users(u1, u2))
        with self.assertRaises(render.RenderError):
            self._render()

    def test_duplicate_hy2_password_is_rejected(self):
        u1 = make_user("U001", 1)
        u2 = make_user("U002", 2)
        u2["hy2_password"] = u1["hy2_password"]
        self._write_users(make_users(u1, u2))
        with self.assertRaises(render.RenderError):
            self._render()

    def test_duplicate_user_id_is_rejected(self):
        self._write_users(make_users(make_user("U001", 1), make_user("U001", 2)))
        with self.assertRaises(render.RenderError):
            self._render()

    def test_zero_live_users_is_rejected(self):
        self._write_users(make_users(make_user("U001", 1, enabled=False)))
        with self.assertRaises(render.RenderError):
            self._render()

    def test_wrong_node_id_is_rejected(self):
        users = make_users(make_user("U001", 1))
        users["node_id"] = "LA01"
        self._write_users(users)
        with self.assertRaises(render.RenderError):
            self._render()

    def test_write_output_does_not_print_secrets(self):
        out = self.root / "config.json.candidate"
        cfg = render.render_config(data_dir=self.data)
        render.write_candidate(cfg, out)
        self.assertTrue(out.is_file())
        raw = out.read_text(encoding="utf-8")
        self.assertIn("vless-reality-in", raw)


if __name__ == "__main__":
    unittest.main()
