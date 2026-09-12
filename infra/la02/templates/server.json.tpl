{
  "log": {
    "level": "info",
    "timestamp": true
  },
  "inbounds": [
    {
      "type": "vless",
      "tag": "vless-reality-in",
      "listen": "0.0.0.0",
      "listen_port": 443,
      "users": [],
      "tls": {
        "enabled": true,
        "server_name": "<REALITY_HANDSHAKE_HOST>",
        "reality": {
          "enabled": true,
          "handshake": {
            "server": "<REALITY_HANDSHAKE_HOST>",
            "server_port": 443
          },
          "private_key": "<LA02_REALITY_PRIVATE_KEY>",
          "short_id": ["<LA02_SHORT_ID>"]
        }
      }
    },
    {
      "type": "hysteria2",
      "tag": "hy2-in",
      "listen": "0.0.0.0",
      "listen_port": 443,
      "users": [],
      "tls": {
        "enabled": true,
        "server_name": "<NODE_FQDN>",
        "certificate_path": "/etc/letsencrypt/live/<NODE_FQDN>/fullchain.pem",
        "key_path": "/etc/letsencrypt/live/<NODE_FQDN>/privkey.pem"
      }
    }
  ]
}
