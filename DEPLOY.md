# auth-service 部署操作手册

## 概述

本服务通过 ArgoCD GitOps 部署，k8s 配置由 `k8s-gitops` 仓库管理。**Secrets 不进 GitOps 仓库**，需在目标集群手动提前创建；ArgoCD 只管理 Deployment / ConfigMap / Service 等无密态资源。

所有敏感配置通过 `envFrom: secretRef` 注入为 OS 环境变量，服务启动时由 Spring Boot 读取，不依赖 Spring Cloud Kubernetes API。

| 环境 | 访问方式 | TLS |
|---|---|---|
| beta | Tailscale 内网，NodePort 30880，HTTP | 无需证书，Tailscale WireGuard 加密 |
| prod | 公网，Istio Gateway，HTTPS | cert-manager 自动签发/续签 |

---

## 手动创建 Secrets（首次部署或凭据轮换时执行）

### Beta 环境

```bash
# 切换到目标命名空间（如不存在先创建）
kubectl create namespace flyingjack-beta --dry-run=client -o yaml | kubectl apply -f -

# 数据库 & 服务连接凭据
kubectl create secret generic auth-connect \
  --from-literal=DB_URL=jdbc:postgresql://beta.flyingcloud.local:5432/auth \
  --from-literal=DB_USER=postgres \
  --from-literal=DB_PASSWORD=<实际密码> \
  --from-literal=RSA_PRIVATE_KEY=<Base64编码的PKCS8私钥> \
  --from-literal=CORS_ALLOWED_ORIGINS=http://beta-auth.flyingjack.top \
  -n flyingjack-beta \
  --dry-run=client -o yaml | kubectl apply -f -

# Redis 凭据
kubectl create secret generic cache-access-secret \
  --from-literal=REDIS_HOST=<beta Redis地址> \
  --from-literal=REDIS_PASSWORD=<Redis密码，无密码则留空字符串> \
  -n flyingjack-beta \
  --dry-run=client -o yaml | kubectl apply -f -
```

### Prod 环境

```bash
kubectl create namespace flyingjack-prod --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic auth-connect \
  --from-literal=DB_URL=jdbc:postgresql://prod.flyingcloud.local:5432/auth \
  --from-literal=DB_USER=<prod数据库用户> \
  --from-literal=DB_PASSWORD=<prod数据库密码> \
  --from-literal=RSA_PRIVATE_KEY=<Base64编码的PKCS8私钥> \
  --from-literal=CORS_ALLOWED_ORIGINS=https://auth.flyingjack.top \
  -n flyingjack-prod \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic cache-access-secret \
  --from-literal=REDIS_HOST=<prod Redis地址> \
  --from-literal=REDIS_PASSWORD=<prod Redis密码> \
  -n flyingjack-prod \
  --dry-run=client -o yaml | kubectl apply -f -
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

### Beta 环境

```bash
# 检查 secret 存在且标签正确
kubectl get secret -n flyingjack-beta -l secret.group=auth-connect
kubectl get secret -n flyingjack-beta -l secret.group=cache-access-secret

# 查看 secret 的 key（不显示值）
kubectl get secret auth-connect -n flyingjack-beta -o jsonpath='{.data}' | python3 -c "import sys,json; [print(k) for k in json.load(sys.stdin)]"
kubectl get secret cache-access-secret -n flyingjack-beta -o jsonpath='{.data}' | python3 -c "import sys,json; [print(k) for k in json.load(sys.stdin)]"
```

### Prod 环境

```bash
# 检查 secret 存在且标签正确
kubectl get secret -n flyingjack-prod -l secret.group=auth-connect
kubectl get secret -n flyingjack-prod -l secret.group=cache-access-secret

# 查看 secret 的 key（不显示值）
kubectl get secret auth-connect -n flyingjack-prod -o jsonpath='{.data}' | python3 -c "import sys,json; [print(k) for k in json.load(sys.stdin)]"
kubectl get secret cache-access-secret -n flyingjack-prod -o jsonpath='{.data}' | python3 -c "import sys,json; [print(k) for k in json.load(sys.stdin)]"
```

---

## Beta 环境访问

Beta 仅限 Tailscale 内网访问，通过 NodePort 30880 暴露，无需 TLS（Tailscale WireGuard 隧道已加密）。

- **访问地址**：`http://100.107.74.15:30880`
- **DNS**：`beta-auth.flyingjack.top` A 记录指向 Tailscale IP `100.107.74.15`（不可公网路由）
- **安全组**：无需开放 30880 端口，公网无法访问；Tailscale 流量通过 WireGuard 绕过安全组

```bash
# 验证
curl http://100.107.74.15:30880/actuator/health
```

---

## 手动申请 TLS 证书（仅 Prod，首次部署时执行）

cert-manager Certificate 资源不走 ArgoCD 管理，需手动在 `istio-system` 命名空间申请一次，之后 cert-manager 自动续签。

> **关于 HTTP→HTTPS 重定向**：Prod Istio Gateway 未配置 `httpsRedirect`，这是有意为之。cert-manager 使用 HTTP-01 方式验证域名，若开启重定向会导致每次续签时 ACME challenge 被 301 拦截，形成死锁。

### Prod 环境

```bash
kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: auth-flyingjack-top-tls
  namespace: istio-system
spec:
  secretName: auth-flyingjack-top-tls
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
    - auth.flyingjack.top
EOF

kubectl get certificate auth-flyingjack-top-tls -n istio-system -w
```

> 证书签发依赖 DNS 已解析到集群入口 IP，Let's Encrypt 能从公网访问对应域名。

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

> **前置条件**：Namespace 须提前手动创建，见 `k8s-gitops/shared/DEPLOY.md`。ArgoCD Application 不负责创建 Namespace，防止 auto-prune 误删命名空间。

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

## 部署验证（浏览器 Smoke Test）

以下两个接口可直接在浏览器中访问，用于快速验证 Prod 环境整条链路是否正常。

### 1. OIDC 发现文档

```
https://auth.flyingjack.top/.well-known/openid-configuration
```

验证点：
- 能返回 JSON 说明 **Istio `/.well-known/` 直通路由**正常
- JSON 中 `"issuer"` 字段必须为 `"https://auth.flyingjack.top"`，否则说明 `AUTH_ISSUER_URI` 环境变量未注入，检查 k8s-gitops 中对应环境的 Secret/ConfigMap

### 2. 登录状态检查

```
https://auth.flyingjack.top/api/account/check-login
```

预期返回：
```json
{ "code": 401, "message": "用户未登录", "data": null }
```

验证点：
- 能返回上述 JSON 说明 **Istio `/api/` 前缀路由**正常（前缀已被剥除，请求正确到达 auth-service）
- 若返回 502/503，检查 Pod 是否正常运行：`kubectl get pods -n flyingjack-prod`

---

## 注意事项

- `.env.secret` 文件**不得**提交到 `k8s-gitops` 仓库，gitops 仓库中不存在这些文件属于正常状态。
- Secret 变更后需手动 `kubectl rollout restart deployment/auth-service-v1 -n <namespace>` 触发 Pod 重启以读取新值。
- prod 环境禁止直接 `kubectl apply`，所有变更须通过 ArgoCD 同步。
