#!/bin/bash
# Phase 1 on LA02. Idempotent enough to re-run. Does not disable password SSH.
set -eu
PUBKEY='ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAINJ3gZiPrKxquvk2RjBDW0QwXJyFk/jYH+ePimnGQvOb la02-deploy@local'

echo "PHASE1_START $(hostname) $(date -u +%Y-%m-%dT%H:%M:%SZ)"
hostnamectl set-hostname la02
grep -qE '^127\.0\.1\.1[[:space:]]+la02(\s|$)' /etc/hosts || echo '127.0.1.1 la02' >> /etc/hosts

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get -y -o Dpkg::Options::=--force-confold upgrade
apt-get install -y curl ca-certificates gnupg jq openssl ufw certbot python3-certbot-dns-cloudflare dnsutils mtr-tiny unattended-upgrades

if ! id -u deploy >/dev/null 2>&1; then
  adduser --disabled-password --gecos '' deploy
fi
usermod -aG sudo deploy
install -d -m 700 -o deploy -g deploy /home/deploy/.ssh
printf '%s\n' "$PUBKEY" > /home/deploy/.ssh/authorized_keys
chown deploy:deploy /home/deploy/.ssh/authorized_keys
chmod 600 /home/deploy/.ssh/authorized_keys

install -d -m 700 -o root -g root /root/.ssh
if [ -f /root/.ssh/authorized_keys ]; then
  grep -qxF "$PUBKEY" /root/.ssh/authorized_keys || printf '%s\n' "$PUBKEY" >> /root/.ssh/authorized_keys
else
  printf '%s\n' "$PUBKEY" > /root/.ssh/authorized_keys
fi
chmod 600 /root/.ssh/authorized_keys

cat >/etc/sudoers.d/deploy <<'EOF'
deploy ALL=(ALL) NOPASSWD:ALL
EOF
chmod 440 /etc/sudoers.d/deploy
visudo -cf /etc/sudoers.d/deploy

printf 'vm.swappiness=10\n' > /etc/sysctl.d/60-la02-memory.conf
sysctl --system >/tmp/la02-sysctl.out

cat >/etc/apt/apt.conf.d/51-la02-unattended <<'EOF'
Unattended-Upgrade::Automatic-Reboot "false";
Unattended-Upgrade::Automatic-Reboot-WithUsers "false";
Unattended-Upgrade::Allowed-Origins {
        "${distro_id}:${distro_codename}-security";
        "${distro_id}ESMApps:${distro_codename}-apps-security";
        "${distro_id}ESM:${distro_codename}-infra-security";
};
EOF

sshd -t
echo "SWAP=$(swapon --show --noheadings | awk '{print $1,$3}' | tr '\n' ';')"
echo "SWAPPINESS=$(sysctl -n vm.swappiness)"
echo "REBOOT_REQUIRED=$([ -f /var/run/reboot-required ] && echo yes || echo no)"
if [ -f /var/run/reboot-required.pkgs ]; then cat /var/run/reboot-required.pkgs; fi
dpkg --audit || true
systemctl --failed --no-pager || true
echo "PHASE1_PACKAGES_OK"
echo "PHASE1_END $(hostname) $(date -u +%Y-%m-%dT%H:%M:%SZ)"
