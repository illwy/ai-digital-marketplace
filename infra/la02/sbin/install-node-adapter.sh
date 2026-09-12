#!/bin/sh
# Copy LA02 node-adapter files. Run as root on LA02 only.
# Does not start sing-box, write secrets, or change SSH/UFW.
set -eu

if [ "$(id -u)" -ne 0 ]; then
  echo "install-node-adapter: must run as root" >&2
  exit 1
fi

HOSTNAME_NOW="$(hostname -s 2>/dev/null || hostname)"
IP_NOW="$(ip -4 -o addr show scope global | awk '{print $4}' | cut -d/ -f1 | head -n1 || true)"
if [ "$HOSTNAME_NOW" != "la02" ] && [ "$IP_NOW" != "45.63.95.89" ]; then
  echo "install-node-adapter: refuse to install off LA02 (host=$HOSTNAME_NOW ip=$IP_NOW)" >&2
  exit 1
fi

ROOT="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"

install -d -m 755 /usr/local/libexec/la02
install -d -m 755 /usr/local/sbin
install -d -m 755 /etc/sing-box/templates
install -d -m 700 /etc/sing-box/data /var/backups/la02-sing-box /root/la02-client-info
install -d -m 750 /usr/local/libexec/la02

install -m 644 "$ROOT/libexec/la02/__init__.py" /usr/local/libexec/la02/__init__.py
install -m 644 "$ROOT/libexec/la02/render.py" /usr/local/libexec/la02/render.py
install -m 644 "$ROOT/libexec/la02/apply.py" /usr/local/libexec/la02/apply.py
install -m 644 "$ROOT/libexec/la02/export.py" /usr/local/libexec/la02/export.py
install -m 644 "$ROOT/libexec/la02/health.py" /usr/local/libexec/la02/health.py
install -m 644 "$ROOT/templates/server.json.tpl" /etc/sing-box/templates/server.json.tpl

install -m 750 "$ROOT/sbin/la02-render-config" /usr/local/sbin/la02-render-config
install -m 750 "$ROOT/sbin/la02-apply-config" /usr/local/sbin/la02-apply-config
install -m 750 "$ROOT/sbin/la02-export-client" /usr/local/sbin/la02-export-client
install -m 750 "$ROOT/sbin/la02-health" /usr/local/sbin/la02-health
install -m 750 "$ROOT/sbin/la02-init-secrets" /usr/local/sbin/la02-init-secrets

install -d -m 755 /etc/systemd/system
install -m 644 "$ROOT/../systemd/la02-health.service" /etc/systemd/system/la02-health.service
install -m 644 "$ROOT/../systemd/la02-health.timer" /etc/systemd/system/la02-health.timer

echo "install-node-adapter: files installed; sing-box not started"
