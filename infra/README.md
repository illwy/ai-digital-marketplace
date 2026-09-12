# Node infrastructure

This directory contains the deploy-time assets imported from the former tizi workspace.

- la02/: node adapter scripts, configuration templates, remote deployment helpers, and tests.
- systemd/: service and timer units for node health checks and related operations.

The marketplace backend remains the system of record for products, orders, payments, and node subscriptions. These files are operational assets used to provision and maintain node servers; they are not a second marketplace backend.

Do not place private keys, real tokens, passwords, or production .env files in this repository.

The installer under la02/sbin/ resolves shared systemd units from infra/systemd/.

## Tests

From the repository root:

```bash
python infra/la02/run_tests.py
```

The runner puts `infra/la02` and `infra/la02/libexec` on `sys.path`, so the suite does not need a manually exported `PYTHONPATH`.
