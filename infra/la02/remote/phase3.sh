#!/bin/bash
# Phase 3: install and hold sing-box 1.13.21. Do not start production listeners.
set -eu

install -d -m 755 /etc/apt/keyrings
curl -fsSL https://sing-box.app/gpg.key -o /etc/apt/keyrings/sagernet.asc
chmod a+r /etc/apt/keyrings/sagernet.asc
install -m 644 /dev/null /etc/apt/sources.list.d/sagernet.sources
cat >/etc/apt/sources.list.d/sagernet.sources <<'EOF'
Types: deb
URIs: https://deb.sagernet.org/
Suites: *
Components: *
Enabled: yes
Signed-By: /etc/apt/keyrings/sagernet.asc
EOF

apt-get update
echo '--- madison ---'
apt-cache madison sing-box || true
echo '--- policy ---'
apt-cache policy sing-box || true

EXACT="$(apt-cache madison sing-box | awk '$3 ~ /^1\.13\.21/ {print $3; exit}')"
if [ -z "$EXACT" ]; then
  echo 'SINGBOX_VERSION_MISSING 1.13.21 not in repo; stop'
  apt-cache madison sing-box || true
  exit 2
fi
echo "SB_DEB_VERSION=$EXACT"
DEBIAN_FRONTEND=noninteractive apt-get install -y "sing-box=${EXACT}"
sing-box version
apt-mark hold sing-box
systemctl stop sing-box || true
systemctl disable --now sing-box || true
echo '--- unit ---'
systemctl cat sing-box
echo '--- show ---'
systemctl show sing-box -p User -p Group -p ExecStart -p FragmentPath -p WorkingDirectory

install -d -m 755 /etc/sing-box/templates
install -d -m 700 /etc/sing-box/data /var/backups/la02-sing-box /root/la02-client-info
install -d -m 750 /usr/local/libexec/la02
echo 'HOLD='
apt-mark showhold
echo 'LISTEN443='
ss -lntup | grep ':443' || echo none
echo PHASE3_OK
