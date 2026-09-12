# Node infrastructure

This directory contains the deploy-time assets imported from the former tizi workspace.

- la02/: node adapter scripts, configuration templates, remote deployment helpers, and tests.
- systemd/: service and timer units for node health checks and related operations.

The marketplace backend remains the system of record for products, orders, payments, and node subscriptions. These files are operational assets used to provision and maintain node servers; they are not a second marketplace backend.

Do not place private keys, real tokens, passwords, or production .env files in this repository.
