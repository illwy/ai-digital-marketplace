#!/bin/bash
# Phase 5: issue ECDSA cert and open 443. Requires /root/.secrets/cloudflare.ini.
set -eu
FQDN="${LA02_FQDN:-la02.k8izuh.com}"
EMAIL="${ACME_EMAIL:-yzrhw0218@163.com}"
INI=/root/.secrets/cloudflare.ini

test -f "$INI"
test "$(stat -c %a "$INI")" = "600"

certbot certonly \
  --dns-cloudflare \
  --dns-cloudflare-credentials "$INI" \
  --dns-cloudflare-propagation-seconds 60 \
  --key-type ecdsa \
  -d "$FQDN" \
  -m "$EMAIL" \
  --agree-tos \
  --non-interactive

certbot certificates
systemctl enable --now certbot.timer
certbot renew --dry-run

echo '--- cert paths ---'
namei -l "/etc/letsencrypt/live/${FQDN}/fullchain.pem"
namei -l "/etc/letsencrypt/live/${FQDN}/privkey.pem"
openssl x509 -in "/etc/letsencrypt/live/${FQDN}/fullchain.pem" -noout -subject -issuer -dates

SSH_PORT="$(sshd -T | awk '$1=="port"{print $2; exit}')"
echo "SSH_PORT=${SSH_PORT}"
ufw status verbose || true
ufw allow "${SSH_PORT}/tcp" comment 'SSH admin' || true
ufw allow 443/tcp comment 'LA02 VLESS Reality'
ufw allow 443/udp comment 'LA02 Hysteria2'
ufw --force enable
ufw status numbered

echo PHASE5_CERTS_OK
