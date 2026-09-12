#!/usr/bin/env python3
"""OpenSSH ProxyCommand helper: stdio <-> SOCKS5(127.0.0.1:10808)."""
from __future__ import print_function

import select
import socket
import struct
import sys


def socks5_connect(dest_host, dest_port, proxy_host="127.0.0.1", proxy_port=10808):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(20)
    sock.connect((proxy_host, proxy_port))
    sock.sendall(b"\x05\x01\x00")
    resp = sock.recv(2)
    if resp != b"\x05\x00":
        raise SystemExit("SOCKS5 auth failed: %r" % (resp,))
    host_b = dest_host.encode("ascii")
    req = b"\x05\x01\x00\x03" + bytes([len(host_b)]) + host_b + struct.pack("!H", dest_port)
    sock.sendall(req)
    hdr = sock.recv(4)
    if len(hdr) < 4 or hdr[0] != 5 or hdr[1] != 0:
        raise SystemExit("SOCKS5 connect failed: %r" % (hdr,))
    atyp = hdr[3]
    if atyp == 1:
        sock.recv(6)
    elif atyp == 3:
        ln = sock.recv(1)[0]
        sock.recv(ln + 2)
    elif atyp == 4:
        sock.recv(18)
    sock.settimeout(None)
    return sock


def relay(sock):
    stdin = sys.stdin.buffer if hasattr(sys.stdin, "buffer") else sys.stdin
    stdout = sys.stdout.buffer if hasattr(sys.stdout, "buffer") else sys.stdout
    try:
        fd_in = stdin.fileno()
    except Exception:
        fd_in = None
    while True:
        r, _, _ = select.select([sock, sys.stdin], [], [])
        if sock in r:
            data = sock.recv(65536)
            if not data:
                return
            stdout.write(data)
            stdout.flush()
        if sys.stdin in r:
            data = stdin.read(65536)
            if not data:
                return
            sock.sendall(data)


def main():
    if len(sys.argv) < 3:
        raise SystemExit("usage: socks_proxycmd.py <host> <port>")
    host = sys.argv[1]
    port = int(sys.argv[2])
    sock = socks5_connect(host, port)
    try:
        relay(sock)
    finally:
        sock.close()


if __name__ == "__main__":
    main()
