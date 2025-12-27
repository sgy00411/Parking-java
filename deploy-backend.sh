#!/bin/bash
################################################################################
# 后端热更新部署脚本
# 功能：自动构建后端并通过Nginx Upstream实现零停机热更新
# 使用：./deploy-backend.sh
################################################################################

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目路径
PROJECT_DIR="/www/wwwroot/Parking-java"
NGINX_CONF="/www/server/panel/vhost/nginx/122.51.64.122.conf"
JAR_FILE="target/quaer_api-0.0.1-SNAPSHOT.jar"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  后端热更新部署开始${NC}"
echo -e "${GREEN}========================================${NC}"

cd "$PROJECT_DIR"

# 1. 检测当前主服务端口
CURRENT_PRIMARY=$(grep -A 5 "upstream backend_app" "$NGINX_CONF" | grep "weight=1" | grep -oP ":\d+" | tr -d ':')

if [ -z "$CURRENT_PRIMARY" ]; then
    echo -e "${RED}错误: 无法检测当前主服务端口${NC}"
    exit 1
fi

echo -e "${YELLOW}当前主服务端口: $CURRENT_PRIMARY${NC}"

# 确定新服务端口
if [ "$CURRENT_PRIMARY" == "8086" ]; then
    NEW_PORT=8087
    OLD_PORT=8086
    NEW_CONFIG="application-8087.yml"
else
    NEW_PORT=8086
    OLD_PORT=8087
    NEW_CONFIG="application.yml"
fi

echo -e "${GREEN}新版本将部署到端口: $NEW_PORT${NC}"

# 2. 拉取最新代码（可选）
# echo -e "${YELLOW}拉取最新代码...${NC}"
# git pull origin main

# 3. 构建项目
echo -e "${YELLOW}开始构建后端项目...${NC}"
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}构建失败，终止部署${NC}"
    exit 1
fi

echo -e "${GREEN}构建成功${NC}"

# 4. 停止旧的备用服务（如果存在）
echo -e "${YELLOW}检查端口 $NEW_PORT ...${NC}"
PID=$(lsof -ti:$NEW_PORT || true)
if [ ! -z "$PID" ]; then
    echo -e "${YELLOW}停止端口 $NEW_PORT 上的旧服务 (PID: $PID)...${NC}"
    kill -15 $PID
    sleep 3
    # 强制杀死如果还在运行
    if kill -0 $PID 2>/dev/null; then
        kill -9 $PID
    fi
fi

# 5. 启动新服务
echo -e "${YELLOW}启动新服务 (端口 $NEW_PORT)...${NC}"
mkdir -p logs

if [ "$NEW_PORT" == "8087" ]; then
    nohup java -jar "$JAR_FILE" \
        --spring.config.location=classpath:/application-8087.yml \
        > logs/app-8087.log 2>&1 &
else
    nohup java -jar "$JAR_FILE" \
        > logs/app-8086.log 2>&1 &
fi

NEW_PID=$!
echo -e "${GREEN}新服务已启动 (PID: $NEW_PID, 端口: $NEW_PORT)${NC}"

# 6. 健康检查
echo -e "${YELLOW}等待服务启动...${NC}"
sleep 10

for i in {1..30}; do
    if lsof -ti:$NEW_PORT > /dev/null 2>&1; then
        echo -e "${GREEN}服务健康检查通过 (端口 $NEW_PORT 已监听)${NC}"
        break
    fi

    if [ $i -eq 30 ]; then
        echo -e "${RED}健康检查失败：服务未能在60秒内启动${NC}"
        echo -e "${RED}查看日志: tail -f logs/app-$NEW_PORT.log${NC}"
        exit 1
    fi

    echo -e "${YELLOW}等待中... ($i/30)${NC}"
    sleep 2
done

# 7. 备份nginx配置
cp "$NGINX_CONF" "$NGINX_CONF.backup-$(date +%Y%m%d-%H%M%S)"

# 8. 切换Nginx Upstream权重
echo -e "${YELLOW}切换Nginx流量到端口 $NEW_PORT ...${NC}"

# 修改配置：将旧端口设为down，新端口设为weight=1
sed -i "s|server 127.0.0.1:$OLD_PORT weight=1|server 127.0.0.1:$OLD_PORT down|g" "$NGINX_CONF"
sed -i "s|server 127.0.0.1:$NEW_PORT down|server 127.0.0.1:$NEW_PORT weight=1|g" "$NGINX_CONF"

# 测试nginx配置
/www/server/nginx/sbin/nginx -t
if [ $? -ne 0 ]; then
    echo -e "${RED}Nginx配置测试失败，回滚配置${NC}"
    cp "$NGINX_CONF.backup-$(date +%Y%m%d-%H%M%S)" "$NGINX_CONF"
    kill -15 $NEW_PID
    exit 1
fi

# 重载nginx
/www/server/nginx/sbin/nginx -s reload
echo -e "${GREEN}Nginx已切换到新服务${NC}"

# 9. 等待旧服务处理完请求
echo -e "${YELLOW}等待旧服务处理完现有请求...${NC}"
sleep 10

# 10. 停止旧服务
OLD_PID=$(lsof -ti:$OLD_PORT || true)
if [ ! -z "$OLD_PID" ]; then
    echo -e "${YELLOW}停止旧服务 (PID: $OLD_PID, 端口: $OLD_PORT)...${NC}"
    kill -15 $OLD_PID
    sleep 3
    if kill -0 $OLD_PID 2>/dev/null; then
        kill -9 $OLD_PID
    fi
    echo -e "${GREEN}旧服务已停止${NC}"
fi

# 11. 显示部署结果
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  后端热更新部署完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "当前主服务: ${GREEN}端口 $NEW_PORT (PID: $NEW_PID)${NC}"
echo -e "已停止服务: ${YELLOW}端口 $OLD_PORT${NC}"
echo ""
echo -e "服务状态检查:"
echo -e "  - netstat -tlnp | grep java"
echo -e "  - tail -f logs/app-$NEW_PORT.log"
echo ""
echo -e "如需回滚，执行:"
echo -e "  ${YELLOW}# 1. 重新启动旧端口${NC}"
echo -e "  ${YELLOW}# 2. 切换nginx权重回 $OLD_PORT${NC}"
echo -e "  ${YELLOW}# 3. nginx -s reload${NC}"
