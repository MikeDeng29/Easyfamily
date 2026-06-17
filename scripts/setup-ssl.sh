#!/usr/bin/env bash
# 购买域名后运行此脚本完成 HTTPS 配置
# 用法: DOMAIN=yourdomain.com bash scripts/setup-ssl.sh
#
# 前提：域名 A 记录已指向 47.102.126.67

set -euo pipefail

DOMAIN="${DOMAIN:?请设置 DOMAIN 环境变量，例如: DOMAIN=yourdomain.com bash scripts/setup-ssl.sh}"
SERVER_IP="47.102.126.67"
SSH_PORT="52521"
SSH="ssh -p ${SSH_PORT} root@${SERVER_IP}"

echo "==> 安装 certbot"
$SSH "apt-get install -y certbot python3-certbot-nginx"

echo "==> 更新 Nginx server_name"
$SSH "sed -i 's/server_name _;/server_name ${DOMAIN};/' /etc/nginx/sites-available/easyfamily && nginx -t && systemctl reload nginx"

echo "==> 申请 Let's Encrypt 证书"
$SSH "certbot --nginx -d ${DOMAIN} --non-interactive --agree-tos -m jackcdeng@gmail.com"

echo "==> 证书自动续期测试"
$SSH "certbot renew --dry-run"

echo ""
echo "==> 完成！后端 HTTPS 地址：https://${DOMAIN}"
echo "==> 请更新 iOS Config.swift："
echo "    static let apiBaseURL = \"https://${DOMAIN}\""
echo "==> 然后把 Info.plist 中的 NSAllowsArbitraryLoads 改为 false"
