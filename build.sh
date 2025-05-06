#!/bin/bash

# 定义变量（请根据实际情况修改）
DOCKER_IMAGE_NAME="flyingjack-auth-service"      # 镜像名称
DEFAULT_TAG="latest"            # 默认tag
DOCKER_REGISTRY="registry.wms.com" # 镜像仓库地址，如docker.io/yourname
DOCKERFILE_PATH="."             # Dockerfile路径
MVN_PROFILE="-PBeta"                  # Maven profile，如有需要可设置为"-Pprod"

# 预检查 检查Docker Registry访问权限
echo "预检查: 检查Docker Registry访问权限..."
if ! docker login $DOCKER_REGISTRY > /dev/null 2>&1; then
    echo "错误：无法访问Docker Registry $DOCKER_REGISTRY，请检查："
    echo "1. Registry地址是否正确"
    echo "2. 是否已执行 docker login"
    echo "3. 网络连接是否正常"
    exit 1
fi
echo "Docker Registry访问权限验证通过"

# 0. 确认MVN_PROFILE已设置
if [ -z "$MVN_PROFILE" ]; then
    echo "错误：MVN_PROFILE未设置，请编辑脚本设置Maven profile（如-Pprod）"
    exit 1
fi
echo "当前Maven profile: $MVN_PROFILE"

# 1. 询问是否跳过测试
read -p "步骤1: 是否跳过测试? (y/n) " SKIP_TESTS
if [[ $SKIP_TESTS =~ ^[Yy]$ ]]; then
    MVN_SKIP_TESTS="-DskipTests"
    echo "将跳过测试..."
else
    MVN_SKIP_TESTS=""
    echo "将执行测试..."
fi

# 2. 执行mvn clean package进行打包
echo "步骤2: 执行mvn clean package..."
mvn clean package $MVN_PROFILE $MVN_SKIP_TESTS
if [ $? -ne 0 ]; then
    echo "mvn打包失败，请检查错误!"
    exit 1
fi

# 3. 执行docker build进行镜像构建
echo "步骤3: 执行docker build..."
docker build -t $DOCKER_IMAGE_NAME:$DEFAULT_TAG $DOCKERFILE_PATH
if [ $? -ne 0 ]; then
    echo "docker构建失败，请检查错误!"
    exit 1
fi

# 4.0 询问是否上传镜像
read -p "步骤4.0: 是否要上传镜像到仓库? (y/n) " UPLOAD_CHOICE
if [[ ! $UPLOAD_CHOICE =~ ^[Yy]$ ]]; then
    echo "已选择不上传镜像，脚本结束。"
    exit 0
fi

# 4 tag远程镜像
echo "步骤4.1 当前镜像tag为: $DEFAULT_TAG"
read -p "请输入要远程镜像库上传的tag(留空使用默认tag $DEFAULT_TAG): " CUSTOM_TAG
FINAL_TAG=${CUSTOM_TAG:-$DEFAULT_TAG}

echo "步骤4.2 执行docker tag..."
TAGGED_IMAGE="$DOCKER_REGISTRY/$DOCKER_IMAGE_NAME:$FINAL_TAG"
docker tag $DOCKER_IMAGE_NAME:$DEFAULT_TAG $TAGGED_IMAGE
if [ $? -ne 0 ]; then
    echo "docker tag失败，请检查错误!"
    exit 1
fi

# 5 推送远程镜像
echo "步骤5 执行docker push..."
docker push $TAGGED_IMAGE
if [ $? -ne 0 ]; then
    echo "docker push失败，请检查错误!"
    exit 1
fi

echo "操作完成!"
echo "镜像已成功上传到: $TAGGED_IMAGE"
