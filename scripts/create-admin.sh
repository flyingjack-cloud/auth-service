#!/usr/bin/env bash
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BLUE}${BOLD}"
echo "  ╔══════════════════════════════════════════╗"
echo "  ║   flyingjack-cloud  超级管理员创建工具   ║"
echo "  ╚══════════════════════════════════════════╝"
echo -e "${NC}"

# ── 检查 Python3 ──────────────────────────────────────────────────────────────
if ! command -v python3 &>/dev/null; then
    echo -e "${RED}错误: 需要 Python 3，请先安装${NC}"
    exit 1
fi

# ── 安装 Python 依赖 ──────────────────────────────────────────────────────────
echo -e "${YELLOW}检查 Python 依赖 (bcrypt, psycopg2-binary)...${NC}"
python3 -c "import bcrypt" 2>/dev/null    || pip3 install bcrypt -q
python3 -c "import psycopg2" 2>/dev/null  || pip3 install psycopg2-binary -q
echo -e "${GREEN}依赖就绪${NC}\n"

# ── 数据库连接配置 ─────────────────────────────────────────────────────────────
echo -e "${BOLD}=== 数据库连接配置 ===${NC}"
read -rp "  主机   [192.168.31.162]: " DB_HOST;  DB_HOST=${DB_HOST:-192.168.31.162}
read -rp "  端口   [5432]:           " DB_PORT;  DB_PORT=${DB_PORT:-5432}
read -rp "  库名   [auth]:           " DB_NAME;  DB_NAME=${DB_NAME:-auth}
read -rp "  用户名 [postgres]:       " DB_USER;  DB_USER=${DB_USER:-postgres}
read -rsp "  密码   [postgres]:      " DB_PASS;  DB_PASS=${DB_PASS:-postgres}
echo -e "\n"

# ── 管理员账号信息 ─────────────────────────────────────────────────────────────
echo -e "${BOLD}=== 管理员账号信息 ===${NC}"

# 用户名（至少5个字符）
while true; do
    read -rp "  用户名 (≥5个字符): " USERNAME
    if [ ${#USERNAME} -lt 5 ]; then
        echo -e "  ${RED}用户名至少需要5个字符，请重试${NC}"
    else
        break
    fi
done

# 密码（至少8位，两次确认）
while true; do
    read -rsp "  密码   (≥8位): " PASSWORD
    echo ""
    if [ ${#PASSWORD} -lt 8 ]; then
        echo -e "  ${RED}密码至少需要8个字符，请重试${NC}"
        continue
    fi
    read -rsp "  确认密码:       " PASSWORD_CONFIRM
    echo ""
    if [ "$PASSWORD" != "$PASSWORD_CONFIRM" ]; then
        echo -e "  ${RED}两次密码不一致，请重试${NC}"
    else
        break
    fi
done

read -rp  "  邮箱   (可选): " EMAIL
read -rp  "  手机号 (可选, 1[3-9]xxxxxxxxx): " PHONE
echo ""

# ── 通过环境变量安全传参给 Python ────────────────────────────────────────────
export _FC_DB_HOST="$DB_HOST"
export _FC_DB_PORT="$DB_PORT"
export _FC_DB_NAME="$DB_NAME"
export _FC_DB_USER="$DB_USER"
export _FC_DB_PASS="$DB_PASS"
export _FC_USERNAME="$USERNAME"
export _FC_PASSWORD="$PASSWORD"
export _FC_EMAIL="$EMAIL"
export _FC_PHONE="$PHONE"

echo -e "${YELLOW}正在创建管理员账号...${NC}"

python3 << 'PYTHON_SCRIPT'
import os, sys, time, random, re

try:
    import bcrypt
    import psycopg2
except ImportError as e:
    print(f"ERROR: 缺少依赖: {e}")
    sys.exit(1)

# ── 读取环境变量 ──────────────────────────────────────────────────────────────
db_host     = os.environ["_FC_DB_HOST"]
db_port     = int(os.environ["_FC_DB_PORT"])
db_name     = os.environ["_FC_DB_NAME"]
db_user     = os.environ["_FC_DB_USER"]
db_pass     = os.environ["_FC_DB_PASS"]
username    = os.environ["_FC_USERNAME"]
raw_passwd  = os.environ["_FC_PASSWORD"]
email       = os.environ["_FC_EMAIL"] or None
phone       = os.environ["_FC_PHONE"] or None

# ── 本地校验 ─────────────────────────────────────────────────────────────────
if len(username) < 5:
    print("ERROR: 用户名至少需要5个字符")
    sys.exit(1)

if len(raw_passwd) < 8:
    print("ERROR: 密码至少需要8个字符")
    sys.exit(1)

EMAIL_RE = re.compile(
    r'^[a-zA-Z0-9.!#$%&\'*+/=?^_`{|}~-]+'
    r'@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?'
    r'(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$'
)
PHONE_RE = re.compile(r'^1[3-9]\d{9}$')

if email and not EMAIL_RE.match(email):
    print(f"ERROR: 邮箱格式不正确: {email}")
    sys.exit(1)

if phone and not PHONE_RE.match(phone):
    print(f"ERROR: 手机号格式不正确（需为中国大陆格式，如 13812345678）: {phone}")
    sys.exit(1)

# ── 雪花 ID 生成（与 SnowflakeIdGeneratorDelegate 算法一致）─────────────────
# START_STMP = 2023-01-01 00:00:00 UTC = 1672531200000 ms
START_STMP      = 1672531200000
SEQUENCE_BITS   = 12
MACHINE_BITS    = 5
DATACENTER_BITS = 5
MACHINE_LEFT    = SEQUENCE_BITS
DATACENTER_LEFT = SEQUENCE_BITS + MACHINE_BITS
TIMESTMP_LEFT   = DATACENTER_LEFT + DATACENTER_BITS

curr_ms      = int(time.time() * 1000)
datacenter_id = 0
machine_id    = 0
sequence      = random.randint(0, 4095)
user_id = (
    ((curr_ms - START_STMP) << TIMESTMP_LEFT)
    | (datacenter_id << DATACENTER_LEFT)
    | (machine_id    << MACHINE_LEFT)
    | sequence
)

# ── BCrypt 加密 ──────────────────────────────────────────────────────────────
hashed_password = bcrypt.hashpw(raw_passwd.encode("utf-8"), bcrypt.gensalt(12)).decode("utf-8")

# ── 写入数据库 ────────────────────────────────────────────────────────────────
try:
    conn = psycopg2.connect(
        host=db_host, port=db_port, dbname=db_name,
        user=db_user, password=db_pass, connect_timeout=10
    )
    conn.autocommit = False
    cur = conn.cursor()

    # 唯一性检查
    cur.execute("SELECT 1 FROM auth_users WHERE username = %s", (username,))
    if cur.fetchone():
        print(f"ERROR: 用户名 '{username}' 已存在")
        sys.exit(1)

    if email:
        cur.execute("SELECT 1 FROM auth_users WHERE email = %s::email", (email,))
        if cur.fetchone():
            print(f"ERROR: 邮箱 '{email}' 已被使用")
            sys.exit(1)

    if phone:
        cur.execute("SELECT 1 FROM auth_users WHERE phone = %s::cn_phone_number", (phone,))
        if cur.fetchone():
            print(f"ERROR: 手机号 '{phone}' 已被使用")
            sys.exit(1)

    # 查询 ROLE_ADMIN 的 id
    cur.execute("SELECT id FROM auth_roles WHERE name = 'ROLE_ADMIN'")
    row = cur.fetchone()
    if not row:
        print("ERROR: ROLE_ADMIN 角色不存在，请先执行数据库初始化脚本 (schema.sql)")
        sys.exit(1)
    admin_role_id = row[0]

    # 插入用户
    cur.execute(
        """
        INSERT INTO auth_users (id, username, password, email, phone)
        VALUES (%s, %s, %s, %s, %s)
        """,
        (user_id, username, hashed_password, email, phone)
    )

    # 绑定 ROLE_ADMIN
    cur.execute(
        "INSERT INTO user_role (user_id, role_id) VALUES (%s, %s)",
        (user_id, admin_role_id)
    )

    conn.commit()
    cur.close()
    conn.close()

    print("OK")
    print(f"ID:    {user_id}")
    print(f"用户名: {username}")
    print(f"邮箱:  {email  or '(未设置)'}")
    print(f"手机:  {phone  or '(未设置)'}")
    print(f"角色:  ROLE_ADMIN")

except psycopg2.Error as e:
    print(f"ERROR: 数据库操作失败: {e}")
    sys.exit(1)
PYTHON_SCRIPT

PYTHON_EXIT=$?

# 清理环境变量（不让密码残留）
unset _FC_DB_HOST _FC_DB_PORT _FC_DB_NAME _FC_DB_USER _FC_DB_PASS
unset _FC_USERNAME _FC_PASSWORD _FC_EMAIL _FC_PHONE

echo ""
if [ $PYTHON_EXIT -eq 0 ]; then
    echo -e "${GREEN}${BOLD}✓ 管理员账号创建成功！${NC}"
else
    echo -e "${RED}${BOLD}✗ 创建失败，请检查上方错误信息${NC}"
    exit 1
fi
