"""SSH to LA02 through a local SOCKS5 proxy. Password is never printed."""
from __future__ import print_function

import os
import socket
import struct

import paramiko

DEFAULT_PROXY = ("127.0.0.1", 10808)
LA02_HOST = "45.63.95.89"
LA02_USER = "root"


def socks5_connect(dest_host, dest_port, proxy_host=DEFAULT_PROXY[0], proxy_port=DEFAULT_PROXY[1], timeout=20):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(timeout)
    sock.connect((proxy_host, proxy_port))
    sock.sendall(b"\x05\x01\x00")
    resp = sock.recv(2)
    if resp != b"\x05\x00":
        sock.close()
        raise RuntimeError("SOCKS5 auth failed: %r" % (resp,))
    host_b = dest_host.encode("ascii")
    req = b"\x05\x01\x00\x03" + bytes([len(host_b)]) + host_b + struct.pack("!H", dest_port)
    sock.sendall(req)
    hdr = sock.recv(4)
    if len(hdr) < 4 or hdr[0] != 5 or hdr[1] != 0:
        sock.close()
        raise RuntimeError("SOCKS5 connect failed: %r" % (hdr,))
    atyp = hdr[3]
    if atyp == 1:
        sock.recv(6)
    elif atyp == 3:
        ln = sock.recv(1)[0]
        sock.recv(ln + 2)
    elif atyp == 4:
        sock.recv(18)
    return sock


def connect_la02(password=None, host=LA02_HOST, username=LA02_USER, key_filename=None):
    sock = socks5_connect(host, 22)
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    kwargs = dict(
        hostname=host,
        username=username,
        sock=sock,
        timeout=20,
        banner_timeout=20,
        auth_timeout=20,
        allow_agent=False,
        look_for_keys=False,
    )
    if key_filename:
        kwargs["key_filename"] = key_filename
    else:
        if password is None:
            password = os.environ.get("LA02_SSH_PASSWORD")
        if not password:
            raise RuntimeError("LA02_SSH_PASSWORD is not set")
        kwargs["password"] = password
    client.connect(**kwargs)
    return client


def run(client, command, timeout=60):
    stdin, stdout, stderr = client.exec_command(command, timeout=timeout)
    out = stdout.read().decode("utf-8", "replace")
    err = stderr.read().decode("utf-8", "replace")
    code = stdout.channel.recv_exit_status()
    return code, out, err
