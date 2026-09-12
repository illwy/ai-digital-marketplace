const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  HeadingLevel, WidthType, BorderStyle, ShadingType, AlignmentType } = require("docx");
const fs = require("fs");

const out = "D:/claude/tizi/doc/la02/LA02_交付手册_新机_2026-09-05.docx";
const navy = "1B365D";
const border = { style: BorderStyle.SINGLE, size: 4, color: "C5CDD6" };

function text(value, opts = {}) {
  return new Paragraph({
    spacing: { after: opts.after ?? 100, line: 276 },
    alignment: opts.align,
    children: [new TextRun({ text: value, font: "Microsoft YaHei", size: opts.size ?? 21, color: opts.color ?? "222222", bold: opts.bold })],
  });
}
function heading(level, value) {
  return new Paragraph({
    heading: level,
    spacing: { before: 240, after: 100 },
    children: [new TextRun({ text: value, font: "Microsoft YaHei", size: level === HeadingLevel.HEADING_1 ? 30 : 24, color: navy, bold: true })],
  });
}
function table(rows) {
  return new Table({
    width: { size: 9638, type: WidthType.DXA },
    columnWidths: [2800, 6838],
    rows: rows.map((row, index) => new TableRow({ children: row.map((value, col) => new TableCell({
      width: { size: col ? 6838 : 2800, type: WidthType.DXA },
      borders: { top: border, bottom: border, left: border, right: border },
      shading: index === 0 ? { type: ShadingType.CLEAR, fill: navy } : undefined,
      children: [new Paragraph({ spacing: { after: 0, line: 240 }, children: [new TextRun({ text: value, font: "Microsoft YaHei", size: 18, color: index === 0 ? "FFFFFF" : "222222", bold: index === 0 || col === 0 })] })],
    })) })) ,
  });
}

const doc = new Document({
  styles: { default: { document: { run: { font: "Microsoft YaHei", size: 21 } } } },
  sections: [{
    properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1134, bottom: 1134, left: 1134, right: 1134 } } },
    children: [
      text("LA02 自建节点 · 当前交付版本", { size: 20, color: "555555", after: 40 }),
      text("新机交付手册", { size: 46, bold: true, color: navy, after: 80 }),
      text("sing-box 1.13.21 · VLESS Reality TCP 443 · Hysteria2 UDP 443", { size: 20, color: "555555", after: 180 }),
      text("安全边界：本文不记录 UUID、Reality 私钥/公钥、Hysteria2 密码、Cloudflare Token、SSH 私钥或 root 密码。所有节点凭据在新机上重新生成。", { size: 20, after: 180 }),

      heading(HeadingLevel.HEADING_1, "1. 交付结论"),
      text("LA02 已从旧 VPS 完整迁移到新服务器，国内直连与客户端实测均通过。LA01（nvla.k8izuh.com）保持不变。"),
      table([
        ["项目", "结果"],
        ["服务器", "45.63.95.89 · Ubuntu 24.04.4 LTS · hostname=la02"],
        ["域名", "la02.k8izuh.com · Cloudflare DNS only · A=45.63.95.89 · 无 AAAA"],
        ["协议", "VLESS Reality TCP 443；Hysteria2 UDP 443"],
        ["连通性", "工作站直连两协议均通过，代理出口均为 45.63.95.89"],
        ["旧节点", "LA01 nvla.k8izuh.com = 104.238.140.225，未修改"],
      ]),

      heading(HeadingLevel.HEADING_1, "2. 服务与安全状态"),
      table([
        ["检查项", "当前状态"],
        ["sing-box", "1.13.21，apt hold，systemd enabled/active"],
        ["监听", "0.0.0.0:443 TCP 与 UDP"],
        ["防火墙", "UFW 放行 22/tcp、443/tcp、443/udp；默认入站拒绝"],
        ["SSH", "deploy/root ed25519 登录已验证；密码与 keyboard-interactive 已关闭"],
        ["系统调优", "BBR + fq；vm.swappiness=10；journald 限额已安装"],
        ["证书", "Let's Encrypt ECDSA，la02.k8izuh.com，有效至 2026-12-04；续签 dry-run 通过"],
        ["健康检查", "la02-health.timer 与 certbot.timer 均 active；失败单元数为 0"],
      ]),

      heading(HeadingLevel.HEADING_1, "3. 客户端交付"),
      text("U001 的新客户端文件已从服务器导出到本机受限目录。导入 Clash Verge Rev 时使用 mihomo.yaml；也可使用同目录的 VLESS 或 Hysteria2 URI。"),
      table([
        ["文件", "位置"],
        ["Clash 配置", "C:\\Users\\y\\.secrets\\la02-U001\\mihomo.yaml"],
        ["VLESS URI", "C:\\Users\\y\\.secrets\\la02-U001\\vless.uri"],
        ["Hysteria2 URI", "C:\\Users\\y\\.secrets\\la02-U001\\hysteria2.uri"],
        ["旧文件备份", "C:\\Users\\y\\.secrets\\la02-U001-backup-20260905-195609"],
      ]),
      text("客户端验证结果：VLESS PASS；Hysteria2 PASS；出口均为 45.63.95.89。", { bold: true, after: 160 }),

      heading(HeadingLevel.HEADING_1, "4. 常用维护命令"),
      text("ssh la02"),
      text("sudo systemctl status sing-box --no-pager -l"),
      text("sudo sing-box check -c /etc/sing-box/config.json"),
      text("sudo /usr/local/sbin/la02-apply-config"),
      text("sudo /usr/local/sbin/la02-export-client"),
      text("sudo journalctl -u sing-box -n 80 --no-pager"),
      text("代理开启后访问 https://www.cloudflare.com/cdn-cgi/trace，ip= 应为当前公网 IPv4。"),

      heading(HeadingLevel.HEADING_1, "5. 仓库与凭据边界"),
      text("部署脚本、模板和 systemd 文件位于 D:\\claude\\tizi\\la02\\。服务器上的 config.json、users.json、node-secrets.json、证书和 Cloudflare 凭据不应提交 Git 或粘贴到聊天。"),
      text("新机部署包已重建：D:\\claude\\tizi\\la02\\la02-adapter.tgz。项目状态记录：D:\\claude\\tizi\\la02\\ops\\phase-status.txt。"),
      text("交付日期：2026-09-05。", { color: "555555", after: 0 }),
    ],
  }],
});

Packer.toBuffer(doc).then((buffer) => {
  fs.writeFileSync(out, buffer);
  console.log("WROTE", out, buffer.length);
});
