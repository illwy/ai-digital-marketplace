"""Apply must lock, backup, check, atomically replace, and roll back."""
import json
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from tests.support import NODE_SECRETS, make_user, make_users

import la02.apply as apply
import la02.render as render


class FakeHost(object):
    def __init__(self):
        self.check_ok = True
        self.restart_ok = True
        self.active = True
        self.listening = True
        self.checks = []
        self.restarts = 0
        self.check_paths = []

    def sing_box_check(self, config_path):
        self.check_paths.append(str(config_path))
        self.checks.append(str(config_path))
        if not self.check_ok:
            raise apply.ApplyError("sing-box check failed")

    def restart_service(self):
        self.restarts += 1
        if not self.restart_ok:
            raise apply.ApplyError("restart failed")

    def is_active(self):
        return self.active

    def ports_ok(self):
        return self.listening


class ApplyTests(unittest.TestCase):
    def setUp(self):
        self.tmp = TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.etc = self.root / "etc"
        self.data = self.etc / "data"
        self.backup = self.root / "backups"
        self.lock = self.root / "la02-config.lock"
        self.sbin = self.root / "sbin"
        self.etc.mkdir()
        self.data.mkdir()
        self.backup.mkdir()
        self.sbin.mkdir()
        (self.sbin / "la02-render-config").write_text("render", encoding="utf-8")
        (self.sbin / "la02-apply-config").write_text("apply", encoding="utf-8")
        self.config_path = self.etc / "config.json"
        self.config_path.write_text('{"old": true}\n', encoding="utf-8")
        (self.data / "node-secrets.json").write_text(
            json.dumps(NODE_SECRETS), encoding="utf-8"
        )
        (self.data / "users.json").write_text(
            json.dumps(make_users(make_user("U001", 1))), encoding="utf-8"
        )
        self.host = FakeHost()
        self.paths = apply.Paths(
            config_path=self.config_path,
            data_dir=self.data,
            backup_root=self.backup,
            lock_path=self.lock,
            sbin_dir=self.sbin,
            service_group="sing-box",
        )

    def tearDown(self):
        self.tmp.cleanup()

    def _apply(self):
        return apply.apply_config(self.paths, host=self.host)

    def test_success_replaces_config_and_restarts(self):
        self._apply()
        cfg = json.loads(self.config_path.read_text(encoding="utf-8"))
        self.assertEqual(cfg["inbounds"][0]["tag"], "vless-reality-in")
        self.assertEqual(self.host.restarts, 1)
        self.assertTrue(self.host.check_paths)
        backups = list(self.backup.iterdir())
        self.assertEqual(len(backups), 1)
        saved = (backups[0] / "config.json").read_text(encoding="utf-8")
        self.assertIn('"old": true', saved)

    def test_check_failure_restores_previous_config(self):
        self.host.check_ok = False
        with self.assertRaises(apply.ApplyError):
            self._apply()
        self.assertIn('"old": true', self.config_path.read_text(encoding="utf-8"))
        self.assertEqual(self.host.restarts, 0)

    def test_health_failure_rolls_back_and_restarts_backup(self):
        self.host.listening = False
        with self.assertRaises(apply.ApplyError):
            self._apply()
        self.assertIn('"old": true', self.config_path.read_text(encoding="utf-8"))
        self.assertGreaterEqual(self.host.restarts, 2)

    def test_second_apply_is_rejected_while_lock_held(self):
        lock_fd = apply.acquire_lock(str(self.lock))
        try:
            with self.assertRaises(apply.ApplyError) as ctx:
                self._apply()
            self.assertIn("lock", str(ctx.exception).lower())
        finally:
            apply.release_lock(lock_fd, str(self.lock))

    def test_production_candidate_is_outside_config_directory(self):
        paths = apply.Paths(config_path="/etc/sing-box/config.json")
        self.assertEqual(paths.candidate_path, "/run/la02/config.candidate.json")
        self.assertFalse(paths.candidate_path.startswith("/etc/sing-box/"))

    def test_users_json_is_restored_when_render_fails_after_user_edit(self):
        self._apply()
        bad = make_users(*[make_user("U%03d" % i, i) for i in range(1, 12)])
        (self.data / "users.json").write_text(json.dumps(bad), encoding="utf-8")
        with self.assertRaises(apply.ApplyError):
            self._apply()
        restored = json.loads((self.data / "users.json").read_text(encoding="utf-8"))
        self.assertEqual(len(restored["users"]), 1)
        self.assertEqual(restored["users"][0]["user_id"], "U001")


if __name__ == "__main__":
    unittest.main()
