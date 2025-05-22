#!/bin/bash

set -e  # 如果发生错误，立即退出脚本
set -o pipefail  # 避免错误被隐藏
# set -x  # 启用调试模式，显示每个命令执行情况

# 1. 获取 Maven 版本
DEFAULT_TAG=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

# 2. 定义变量（请根据你的需要修改）
DOCKER_IMAGE_NAME="flyingjack-auth-service"
DOCKER_REGISTRY="registry.wms.com:8443/cloud"
DOCKERFILE_PATH="."
# 读取入参，如果没有传入，则使用默认值 'prod'
MVN_PROFILE=${1:-beta}

echo "当前版本: $DEFAULT_TAG"
echo "当前Maven Profile: $MVN_PROFILE"
echo "即将构建的镜像名 = $DOCKER_IMAGE_NAME"
echo "仓库地址 = $DOCKER_REGISTRY"
echo "构建路径 = $DOCKERFILE_PATH"

read -p "Step 1: Continue with Maven build and Docker process? (y/n): " CONTINUE
if [[ ! "$CONTINUE" =~ ^[Yy]$ ]]; then
    echo "[INFO] User chose to exit. Script completed."
    exit 0
fi

# 3. 是否跳过测试
read -p "Step 1: Skip tests? (y/n): " SKIP_TESTS
MVN_SKIP_TESTS=""
if [[ "$SKIP_TESTS" =~ ^[Yy]$ ]]; then
    MVN_SKIP_TESTS="-DskipTests"
    echo "[INFO] Tests will be skipped..."
else
    echo "[INFO] Tests will be executed..."
fi

# 4. 执行 Maven 构建
echo "Step 2: Running mvn clean package..."
if ! mvn clean package -P "$MVN_PROFILE" $MVN_SKIP_TESTS; then
    echo "[ERROR] Maven build failed!"
    exit 1
fi

# 5. 构建 Docker 镜像
echo "Step 3: Building Docker image..."
if ! docker build -t "$DOCKER_IMAGE_NAME:$DEFAULT_TAG" "$DOCKERFILE_PATH"; then
    echo "[ERROR] Docker build failed!"
    exit 1
fi

# 6. 询问是否上传镜像
read -p "Step 4.0: Upload image to registry? (y/n): " UPLOAD_CHOICE
if [[ ! "$UPLOAD_CHOICE" =~ ^[Yy]$ ]]; then
    echo "[INFO] Image upload skipped. Script completed."
    exit 0
fi

# 7. 允许用户输入自定义标签
echo "当前镜像标签: $DEFAULT_TAG"
read -p "Step 4.1: Enter custom tag (leave blank for default $DEFAULT_TAG): " CUSTOM_TAG
FINAL_TAG="${CUSTOM_TAG:-$DEFAULT_TAG}"

# 8. 执行 Docker Tag
TAGGED_IMAGE="$DOCKER_REGISTRY/$DOCKER_IMAGE_NAME:$FINAL_TAG"
echo "Step 4.2: Tagging Docker image...$TAGGED_IMAGE"
if ! docker tag "$DOCKER_IMAGE_NAME:$DEFAULT_TAG" "$TAGGED_IMAGE"; then
    echo "[ERROR] Docker tag failed!"
    exit 1
fi

# 9. 推送镜像到 Registry
echo "Step 5: Pushing Docker image..."
if ! docker push "$TAGGED_IMAGE"; then
    echo "[ERROR] Docker push failed!"
    exit 1
fi

# 10. 最终输出成功信息
echo "[SUCCESS] Operation completed!"
echo "Image successfully uploaded to: $TAGGED_IMAGE"
