const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, HeadingLevel, BorderStyle, WidthType,
  ShadingType, VerticalAlign, PageNumber } = require("docx");
const fs = require("fs");
const path = require("path");

const PAGE_W = 11906;
const MARGIN = 1134;
const CONTENT_W = PAGE_W - MARGIN * 2; // 9638
const C1 = 2800;
const C2 = CONTENT_W - C1;

const navy = "1B365D";
const ink = "222222";
const muted = "555555";
const rule = "C5CDD6";
const warnBg = "FFF4E5";
const okBg = "E8F5E9";
const badBg = "FDECEA";
const headBg = "1B365D";

const thin = { style: BorderStyle.SINGLE, size: 4, color: rule };
const borders = { top: thin, bottom: thin, left: thin, right: thin };

function p(text, opts = {}) {
  return new Paragraph({
    spacing: { after: opts.after ?? 120, before: opts.before ?? 0, line: 276 },
    alignment: opts.align,
    children: [
      new TextRun({
        text,
        font: "Microsoft YaHei",
        size: opts.size ?? 21,
        color: opts.color ?? ink,
        bold: opts.bold,
        italics: opts.italics,
      }),
    ],
  });
}

function h(level, text) {
  return new Paragraph({
    heading: level,
    spacing: { before: level === HeadingLevel.HEADING_1 ? 280 : 200, after: 120 },
    border: level === HeadingLevel.HEADING_1
      ? { bottom: { style: BorderStyle.SINGLE, size: 12, color: navy, space: 4 } }
      : undefined,
    children: [
      new TextRun({
        text,
        font: "Microsoft YaHei",
        bold: true,
        size: level === HeadingLevel.HEADING_1 ? 32 : 26,
        color: navy,
      }),
    ],
  });
}

function cell(text, width, opts = {}) {
  return new TableCell({
    width: { size: width, type: WidthType.DXA },
    borders,
    verticalAlign: VerticalAlign.CENTER,
    shading: opts.shading
      ? { type: ShadingType.CLEAR, fill: opts.shading }
      : undefined,
    margins: { top: 60, bottom: 60, left: 80, right: 80 },
    children: [
      new Paragraph({
        spacing: { after: 0, line: 240 },
        children: [
          new TextRun({
            text,
            font: "Microsoft YaHei",
            size: opts.size ?? 18,
            bold: !!opts.bold,
            color: opts.color ?? (opts.head ? "FFFFFF" : ink),
          }),
        ],
      }),
    ],
  });
}

function kvTable(rows, head) {
  const header = head
    ? [
        new TableRow({
          children: [
            cell(head[0], C1, { head: true, bold: true, shading: headBg }),
            cell(head[1], C2, { head: true, bold: true, shading: headBg }),
          ],
        }),
      ]
    : [];
  return new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: [C1, C2],
    rows: [
      ...header,
      ...rows.map((r, i) =>
        new TableRow({
          children: [
            cell(r[0], C1, { bold: true, shading: i % 2 ? "F4F7FA" : "FFFFFF" }),
            cell(r[1], C2, { shading: i % 2 ? "F4F7FA" : "FFFFFF" }),
          ],
        })
      ),
    ],
  });
}

function callout(text, fill) {
  return new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: [CONTENT_W],
    rows: [
      new TableRow({
        children: [
          new TableCell({
            width: { size: CONTENT_W, type: WidthType.DXA },
            borders,
            shading: { type: ShadingType.CLEAR, fill },
            margins: { top: 100, bottom: 100, left: 140, right: 140 },
            children: [p(text, { size: 20, after: 0 })],
          }),
        ],
      }),
    ],
  });
}

function code(text) {
  return new Paragraph({
    spacing: { after: 80, line: 260 },
    shading: { type: ShadingType.CLEAR, fill: "F3F4F6" },
    children: [
      new TextRun({
        text,
        font: "Consolas",
        size: 18,
        color: "1A1A1A",
      }),
    ],
  });
}

const doc = new Document({
  styles: {
    default: {
      document: { run: { font: "Microsoft YaHei", size: 21 } },
    },
  },
  sections: [
    {
      properties: {
        page: {
          size: { width: PAGE_W, height: 16838 },
          margin: { top: MARGIN, bottom: MARGIN, left: MARGIN, right: MARGIN },
        },
      },
      headers: {
        default: new Header({
          children: [
            new Paragraph({
              border: { bottom: { style: BorderStyle.SINGLE, size: 8, color: navy, space: 6 } },
              spacing: { after: 80 },
              children: [
                new TextRun({ text: "LA02 交付手册", font: "Microsoft YaHei", size: 18, color: navy, bold: true }),
                new TextRun({ text: "    换机重建 · 不含秘密", font: "Microsoft YaHei", size: 18, color: muted }),
              ],
            }),
          ],
        }),
      },
      footers: {
        default: new Footer({
          children: [
            new Paragraph({
              border: { top: { style: BorderStyle.SINGLE, size: 6, color: rule, space: 8 } },
              alignment: AlignmentType.RIGHT,
              children: [
                new TextRun({ text: "v1.0  2026-09-05    ", font: "Microsoft YaHei", size: 16, color: muted }),
                new TextRun({ children: [PageNumber.CURRENT], font: "Microsoft YaHei", size: 16, color: muted }),
              ],
            }),
          ],
        }),
      },
      children: [
        p("Vultr LA02 自建节点", { size: 20, color: muted, after: 40 }),
        new Paragraph({
          spacing: { after: 80 },
          children: [
            new TextRun({ text: "交付与换机手册", font: "Microsoft YaHei", size: 48, bold: true, color: navy }),
          ],
        }),
        p("sing-box 1.13.21 · VLESS Reality TCP 443 · Hysteria2 UDP 443", { size: 20, color: muted, after: 200 }),

        callout(
          "安全边界：本文不记录 VLESS UUID、Reality 私钥/公钥、Hysteria2 密码、Cloudflare Token、SSH 私钥或 root 密码。尖括号字段在新机上重新生成。",
          warnBg
        ),

        h(HeadingLevel.HEADING_1, "1. 一页结论"),
        p("当前这台 LA02（149.248.21.56）作为「国内直连入口」不合格。节点进程、证书、防火墙在服务器本机是正常的，但中国大陆到该公网 IP 的 TCP 22 与 TCP/UDP 443 均超时（家宽、手机 4G、腾讯云均如此）。旧节点 LA01 同一地区仍可直连。"),
        p("已决定方案 1：销毁/替换这台 VPS，换新公网 IP 后按本文重建。换机期间不要改 LA01 的 DNS、配置或凭据。"),
        kvTable(
          [
            ["判定", "国内入口 No-Go；服务器自身 Go"],
            ["原因", "新 Vultr IP 被墙/被丢，不是 sing-box 崩溃"],
            ["旧节点", "LA01 nvla.k8izuh.com = 104.238.140.225，保持不动"],
            ["下一步", "Vultr 换机拿新 IP → 国内直连 22/443 验收 → 再装代理"],
          ],
          ["项", "值"]
        ),

        h(HeadingLevel.HEADING_1, "2. 当前节点身份（换机前）"),
        kvTable(
          [
            ["节点代号", "LA02"],
            ["域名", "la02.k8izuh.com（Cloudflare DNS only / 灰云）"],
            ["公网 IPv4", "149.248.21.56（即将作废）"],
            ["系统", "Ubuntu 24.04 LTS x64 · 主机名 la02 · UTC"],
            ["规格", "1 vCPU / 1 GB / 25 GB NVMe · Vultr Los Angeles"],
            ["代理核心", "sing-box 1.13.21（apt-mark hold，禁止自动升 1.14）"],
            ["入口", "TCP 443 VLESS Reality；UDP 443 Hysteria2"],
            ["证书", "Let's Encrypt ECDSA，域名 la02.k8izuh.com，约 2026-12-03 到期"],
            ["容量", "最多 10 个 enabled 用户；当前 U001"],
            ["ACME 邮箱", "yzrhw0218@163.com"],
          ],
          ["项", "当前值"]
        ),

        h(HeadingLevel.HEADING_1, "3. 为什么「维护能登、你直连 22 超时」"),
        p("这是两条完全不同的路径，不要混在一起："),
        kvTable(
          [
            ["你的电脑/手机直连", "目标 149.248.21.56:22 和 :443 → 超时。梯子入口走的就是这条。"],
            ["维护登录（Agent）", "本机 127.0.0.1:10808 SOCKS5（现有梯子，出口是 LA01）再转到 LA02:22。只能说明机器还活着。"],
            ["对照 LA01", "同一电脑直连 104.238.140.225 的 22 和 443 都成功。"],
          ],
          ["路径", "结果"]
        ),
        p("Reality / Hysteria2 只能伪装「连上之后长什么样」，挡不住整个 IP 在国内不可达。换客户端软件无法修复。"),

        h(HeadingLevel.HEADING_1, "4. 服务器侧已完成项（证据）"),
        p("以下在 LA02 本机验证通过。它们证明「机内服务正常」，不能证明「国内能当梯子」。"),
        kvTable(
          [
            ["P0–P1", "Ubuntu 基线、hostname=la02、deploy 用户、密钥登录后关密码"],
            ["P2", "A 记录 la02.k8izuh.com → 149.248.21.56；无 AAAA；nvla 仍为 LA01"],
            ["P3", "官方仓库安装 sing-box=1.13.21 并 hold；User=sing-box"],
            ["P4", "render/apply/export/health 脚本；U001 凭据在机上 0600 生成"],
            ["P5", "DNS-01 ECDSA 证书；UFW 22/tcp + 443/tcp + 443/udp；BBR=fq/bbr"],
            ["本机 HY2", "服务器环回 Hysteria2 出网，出口 149.248.21.56"],
            ["VLESS", "国内直连失败；经 LA01 嵌套 Reality 握手无效。未完成国内客户端验收"],
          ],
          ["阶段", "结果"]
        ),

        h(HeadingLevel.HEADING_1, "5. 本机与服务器文件（不含秘密）"),
        p("仓库与脚本可带到新机。秘密只在旧机和本机受限目录，换机后必须全部作废重生成。"),
        kvTable(
          [
            ["仓库适配器", "D:\\claude\\tizi\\la02\\（render/apply/health/模板/systemd）"],
            ["本机 SSH 密钥", "C:\\Users\\y\\.ssh\\la02_ed25519 （公钥已装在旧机 deploy/root）"],
            ["SSH 别名", "~/.ssh/config 中 Host la02，经 SOCKS ProxyCommand"],
            ["CF Token 文件", "C:\\Users\\y\\.secrets\\cf-k8izuh.token（不要提交 Git）"],
            ["U001 客户端副本", "C:\\Users\\y\\.secrets\\la02-U001\\（旧 IP 作废后删除）"],
            ["服务器配置生成物", "/etc/sing-box/config.json（不要 Git）"],
            ["服务器主数据", "/etc/sing-box/data/users.json 与 node-secrets.json，0600"],
            ["客户端导出（旧机）", "/root/la02-client-info/U001/"],
            ["证书", "/etc/letsencrypt/live/la02.k8izuh.com/"],
            ["CF 凭据（旧机）", "/root/.secrets/cloudflare.ini，0600"],
          ],
          ["位置", "说明"]
        ),

        h(HeadingLevel.HEADING_1, "6. 换机前：Vultr 上你要做的"),
        p("在控制台操作，不必再登录旧机装软件。"),
        p("1. 记下旧 IP 149.248.21.56，仅作对照，不要写进新配置当生产入口。"),
        p("2. 销毁或替换实例，开一台新的 Ubuntu 24.04 LTS x64，洛杉矶，建议仍 1C1G。开通后立刻记下新公网 IPv4。"),
        p("3. 平台防火墙（若启用）放行：管理 SSH（先确认实际端口）、443/TCP、443/UDP。"),
        p("4. 不要把新 IP 和 LA01 做成同一个 A 记录轮询。"),
        p("5. 把新 IP 发给部署 Agent。在装代理之前先做国内直连验收。"),

        h(HeadingLevel.HEADING_1, "7. 换机后硬性验收门（先于任何代理安装）"),
        callout(
          "新 IP 若从国内直连 TCP 22 或 TCP 443 仍超时：停止安装 sing-box，不要签发证书，不要改客户端。视为再次抽奖失败。",
          badBg
        ),
        p("必须同时满足："),
        p("家宽直连新 IP:22 成功（或你指定的 SSH 端口）。"),
        p("家宽直连新 IP:443 至少 TCP 能 SYN-ACK（此时还没装代理也可以是 connection refused，但不能是长时间超时）。超时 = 仍被墙。"),
        p("手机 4G 直连同样测一次 22 或 443。"),
        p("对照：nvla.k8izuh.com 仍解析为 104.238.140.225，LA01 客户端不受影响。"),
        p("通过后再改 DNS：Cloudflare 把 la02.k8izuh.com 的 A 记录改成新 IP，必须 DNS only（灰云），不建未验证的 AAAA。"),

        h(HeadingLevel.HEADING_1, "8. 新机重建顺序（Agent 执行）"),
        p("沿用仓库 D:\\claude\\tizi\\la02\\ 与原执行计划，版本仍钉死 1.13.21。凭据全部新生成，禁止从旧机或 LA01 复制 config.json。"),
        kvTable(
          [
            ["P0", "确认新 IP、Ubuntu 24.04、443 空闲、禁止登录 LA01"],
            ["P1", "hostname la02；deploy + 本机公钥；验证密钥后再关密码登录"],
            ["P2", "更新 A 记录到新 IP；1.1.1.1 与 8.8.8.8 只返回新 IP"],
            ["P3", "官方 APT 安装 sing-box=1.13.21 并 hold；服务保持停止"],
            ["P4", "本机生成 Reality 密钥、Short ID、U001 UUID/HY2 密码；安装 render/apply"],
            ["P5", "DNS-01 重签证书（旧证书绑旧 IP，必须重签）；UFW；BBR；apply 后启动"],
            ["P6", "国内直连网络上分别测 VLESS 与 HY2，出口须等于新 IP"],
          ],
          ["阶段", "动作"]
        ),
        p("SSH 加固提醒：Ubuntu cloud-init 的 /etc/ssh/sshd_config.d/50-cloud-init.conf 里 PasswordAuthentication yes 会压过 99- 文件（sshd 取先出现的值）。关密码时必须改 50 或把加固文件排到更前。"),
        p("apply 候选配置必须写在 /run/la02/ 下，不要放进 /etc/sing-box/。官方 unit 使用 sing-box -C /etc/sing-box，会合并该目录下所有 json。"),

        h(HeadingLevel.HEADING_1, "9. 复用与禁止"),
        kvTable(
          [
            ["可复用", "la02 仓库脚本/模板/systemd；域名 la02.k8izuh.com；ACME 邮箱；CF Token（仍仅 Zone Read+DNS Edit）；本机 SSH 公钥"],
            ["必须重做", "公网 IP；Let's Encrypt 证书；Reality 密钥对与 Short ID；每用户 UUID 与 HY2 密码；UFW 按新 SSH 端口放行"],
            ["禁止", "SSH 到 LA01；改 nvla.k8izuh.com；复制 LA01 或旧 LA02 的 config.json；橙云代理该域名；把秘密写入 Git/聊天/手册"],
            ["作废", "旧 IP 的客户端 URI/YAML；旧机 /root/la02-client-info；本机 .secrets/la02-U001（换机成功并验收后再删）"],
          ],
          ["类别", "内容"]
        ),

        h(HeadingLevel.HEADING_1, "10. 新机常用命令（装好之后）"),
        code("ssh la02"),
        p("若新机已可直连，从 ~/.ssh/config 去掉该 Host 的 ProxyCommand，改为 IdentityFile ~/.ssh/la02_ed25519、User deploy。", { size: 19, color: muted }),
        code("sudo systemctl status sing-box --no-pager -l"),
        code("sudo ss -lntup | grep ':443'"),
        code("sudo sing-box check -c /etc/sing-box/config.json"),
        code("sudo /usr/local/sbin/la02-apply-config"),
        code("sudo /usr/local/sbin/la02-export-client"),
        code("sudo journalctl -u sing-box -n 80 --no-pager"),
        p("看出口：代理开启后访问 https://www.cloudflare.com/cdn-cgi/trace ，ip= 必须等于当时的新公网 IPv4。"),

        h(HeadingLevel.HEADING_1, "11. 已接受风险与未决项"),
        p("1C1G 的 10 人是账号上限，不是 10 人同时满速承诺。"),
        p("换新 IP 仍可能一上线就被墙，没有技术手段保证 Vultr 洛杉矶地址在国内长期可达。"),
        p("AI 站点能打开页面不等于账号不被风控。"),
        p("旧机销毁后，未备份的节点私钥无法恢复；新机必须全新生成。离机加密备份若未做，整机丢失即丢全部用户凭据。"),
        p("本文日期 2026-09-05。旧 IP 销毁后，第 2 节中的 149.248.21.56 仅作历史记录。"),

        callout(
          "换机完成标准：国内直连新 IP 的 22 与 443 不再超时；la02.k8izuh.com 只解析到新 IP；sing-box 1.13.21 held；两协议分别出网为新 IP；LA01 仍为 104.238.140.225。",
          okBg
        ),
      ],
    },
  ],
});

const out = path.join("D:/claude/tizi/doc/la02/LA02_交付手册_2026-09-05.docx");
Packer.toBuffer(doc).then((buf) => {
  fs.writeFileSync(out, buf);
  console.log("WROTE", out, buf.length);
});
