# auth-service 部署操作手册

## 概述

本服务通过 ArgoCD GitOps 部署，k8s 配置由 `k8s-gitops` 仓库管理。**Secrets 不进 GitOps 仓库**，需在目标集群手动提前创建；ArgoCD 只管理 Deployment / ConfigMap / Service 等无密态资源。

Spring Cloud Kubernetes 通过 `secret.group` 标签自动发现 Secret 并注入环境变量，因此手动创建时标签必须正确设置。

---

## 手动创建 Secrets（首次部署或凭据轮换时执行）

### Beta 环境

```bash
# 切换到目标命名空间（如不存在先创建）
kubectl create namespace flyingjack-beta --dry-run=client -o yaml | kubectl apply -f -

# 数据库 & 服务连接凭据
kubectl create secret generic auth-connect-secret \
  --from-literal=DB_URL=jdbc:postgresql://beta.flyingcloud.local:5432/auth \
  --from-literal=DB_USER=postgres \
  --from-literal=DB_PASSWORD=<实际密码> \
  --from-literal=RSA_PRIVATE_KEY=<Base64编码的PKCS8私钥> \
  --from-literal=CORS_ALLOWED_ORIGINS=http://<beta域名> \
  -n flyingjack-beta \
  --dry-run=client -o yaml | kubectl apply -f -

# 追加标签（Spring Cloud Kubernetes 通过此标签发现 Secret）
kubectl label secret auth-connect-secret secret.group=auth-connect -n flyingjack-beta --overwrite

# Redis 凭据
kubectl create secret generic redis-access-secret \
  --from-literal=REDIS_HOST=beta.flyingcloud.local \
  --from-literal=REDIS_PASSWORD=<Redis密码，无密码则留空字符串> \
  -n flyingjack-beta \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl label secret redis-access-secret secret.group=cache-access-secret -n flyingjack-beta --overwrite
```

### Prod 环境

```bash
kubectl create namespace flyingjack-prod --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic auth-connect-secret \
  --from-literal=DB_URL=jdbc:postgresql://prod.flyingcloud.local:5432/auth \
  --from-literal=DB_USER=<prod数据库用户> \
  --from-literal=DB_PASSWORD=<prod数据库密码> \
  --from-literal=RSA_PRIVATE_KEY=<Base64编码的PKCS8私钥> \
  --from-literal=CORS_ALLOWED_ORIGINS=https://<prod域名> \
  -n flyingjack-prod \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl label secret auth-connect-secret secret.group=auth-connect -n flyingjack-prod --overwrite

kubectl create secret generic redis-access-secret \
  --from-literal=REDIS_HOST=<prod Redis地址> \
  --from-literal=REDIS_PASSWORD=<prod Redis密码> \
  -n flyingjack-prod \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl label secret redis-access-secret secret.group=cache-access-secret -n flyingjack-prod --overwrite
```

### 镜像仓库拉取凭据（仅认证 registry 需要）

使用无认证的 registry:2 时跳过此步骤。如果 registry 需要认证（如 Harbor），需在每个命名空间创建拉取凭据，并在 `deployment-patch.yaml` 中补充 `imagePullSecrets`：

```bash
kubectl create secret docker-registry harbor-pull-secret \
  --docker-server=<registry地址> \
  --docker-username=<用户名> \
  --docker-password=<密码> \
  -n flyingjack-beta   # prod 环境替换命名空间重复执行
```

---

## RSA 私钥格式说明

`RSA_PRIVATE_KEY` 的值为 **PKCS8 格式、Base64 单行编码**（去掉 PEM header/footer 后 base64）：

```bash
# 生成并编码（如需新密钥）
openssl genrsa -out private.pem 2048
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in private.pem -out private_pkcs8.pem
# 取 PEM body（去掉首尾行）并 base64 编码为单行
grep -v "^-----" private_pkcs8.pem | tr -d '\n'
```

---

## 验证 Secrets 是否正确

```bash
# 检查 secret 存在且标签正确
kubectl get secret -n flyingjack-beta -l secret.group=auth-connect
kubectl get secret -n flyingjack-beta -l secret.group=cache-access-secret

# 查看 secret 的 key（不显示值）
kubectl get secret auth-connect-secret -n flyingjack-beta -o jsonpath='{.data}' | python3 -c "import sys,json; [print(k) for k in json.load(sys.stdin)]"
```

---

## OAuth2 Client 初始化

**首次部署**时，如果数据库里没有任何 OAuth2 client，服务启动会自动用 `application.yml` 里的默认值（`sample-client` / `sample-secret-dev`）写入一条 bootstrap client，并打印日志：

```
No OAuth2 clients found — bootstrapped default client 'sample-client'
```

**首次部署后需立即通过 admin API 创建正式 client，再删除 bootstrap client：**

```bash
# 1. 创建正式 client（通过 auth-service 的 admin 接口）
curl -X POST https://<auth-service地址>/oauth2/clients \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "<正式client-id>",
    "clientSecret": "<强随机密码>",
    "redirectUri": "https://<前端域名>/callback",
    "scopes": ["openid"]
  }'

# 2. 删除 bootstrap client
curl -X DELETE https://<auth-service地址>/oauth2/clients/sample-client
```

之后每次重启，数据库里已有 client，bootstrap 逻辑不再触发。新增 client 统一走 admin API，无需改配置或重启服务。

---

## ArgoCD Application 创建

在 ArgoCD 所在集群执行（或通过 ArgoCD UI 导入）：

```bash
kubectl apply -f - <<EOF
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: auth-service-beta
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/flyingjack-cloud/k8s-gitops
    targetRevision: main
    path: auth-service/overlays/beta
  destination:
    server: https://kubernetes.default.svc
    namespace: flyingjack-beta
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
EOF
```

prod 环境将 `beta` 替换为 `prod` 重复执行。

---

## CI 更新镜像 Tag（GitHub Actions）

每次 CI 构建完成后，需向 `k8s-gitops` 仓库提交镜像 tag 更新：

```yaml
- name: Update image tag in gitops repo
  env:
    GITOPS_TOKEN: ${{ secrets.GITOPS_TOKEN }}
    IMAGE_TAG: beta-${{ github.sha }}
  run: |
    git clone https://x-access-token:${GITOPS_TOKEN}@github.com/flyingjack-cloud/k8s-gitops.git
    cd k8s-gitops/auth-service/overlays/beta
    kustomize edit set image \
      registry.wms.com:8443/cloud/flyingjack-auth-service=registry.wms.com:8443/cloud/flyingjack-auth-service:${IMAGE_TAG}
    git config user.email "ci@flyingjack.com"
    git config user.name "CI Bot"
    git add kustomization.yaml
    git commit -m "chore(auth-service): update image to ${IMAGE_TAG}"
    git push
```

> `GITOPS_TOKEN` 为有 `k8s-gitops` 仓库写权限的 GitHub PAT，存放在 auth-service 仓库的 Actions Secrets 中。

---

## 注意事项

- `.env.secret` 文件**不得**提交到 `k8s-gitops` 仓库，gitops 仓库中不存在这些文件属于正常状态。
- Secret 变更后需手动 `kubectl rollout restart deployment/auth-service-v1 -n <namespace>` 触发 Pod 重启以读取新值。
- prod 环境禁止直接 `kubectl apply`，所有变更须通过 ArgoCD 同步。
