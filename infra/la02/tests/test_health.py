"""Health script exits 0 when the node is healthy, non-zero on faults."""
import unittest

import la02.health as health


class FakeProbe(object):
    def __init__(self):
        self.active = True
        self.tcp443 = True
        self.udp443 = True
        self.disk_pct = 40.0
        self.swap_growing = False
        self.cert_days = 60
        self.oom = False
        self.restarts = 0
        self.check_ok = True

    def is_active(self):
        return self.active

    def tcp_listening(self):
        return self.tcp443

    def udp_listening(self):
        return self.udp443

    def disk_percent(self):
        return self.disk_pct

    def swap_is_growing(self):
        return self.swap_growing

    def cert_days_remaining(self):
        return self.cert_days

    def recent_oom(self):
        return self.oom

    def restart_count(self):
        return self.restarts

    def config_check(self):
        return self.check_ok


class HealthTests(unittest.TestCase):
    def test_healthy_node_exits_zero(self):
        code, problems = health.evaluate(FakeProbe())
        self.assertEqual(code, 0)
        self.assertEqual(problems, [])

    def test_inactive_service_exits_nonzero(self):
        probe = FakeProbe()
        probe.active = False
        code, problems = health.evaluate(probe)
        self.assertNotEqual(code, 0)
        self.assertTrue(problems)

    def test_high_disk_exits_nonzero(self):
        probe = FakeProbe()
        probe.disk_pct = 85.0
        code, problems = health.evaluate(probe)
        self.assertNotEqual(code, 0)

    def test_cert_under_30_days_exits_nonzero(self):
        probe = FakeProbe()
        probe.cert_days = 29
        code, problems = health.evaluate(probe)
        self.assertNotEqual(code, 0)


if __name__ == "__main__":
    unittest.main()
