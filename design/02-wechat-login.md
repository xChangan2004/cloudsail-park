# 微信小程序登录接入说明

## 1. 配置

不要把真实 AppSecret 写进版本库，启动前通过环境变量注入：

```bash
WECHAT_APP_ID=wx...
WECHAT_APP_SECRET=...
```

对应 `application.yml`：

```yaml
wechat:
  app-id: ${WECHAT_APP_ID:}
  app-secret: ${WECHAT_APP_SECRET:}
```

## 2. 登录流程

```text
uni.login() 得到 code
        │
        ▼
POST /app/auth/wx/login  { "code": "..." }
        │
        ├─ openid 已绑定账号 ──► 返回 token
        │
        └─ openid 未绑定账号 ──► 自动创建微信临时账号（phone 为空）
                                 返回 token + needBindPhone=true
                                          │
                                          ▼
                           POST /app/auth/bind-phone        （短信验证码）
                           或 POST /app/auth/bind-phone/wx  （微信一键手机号）
                                          │
                                          ▼
                                   绑定手机号成功
```

新用户首次微信登录会先得到一个 `phone` 为空的临时账号，因此 `customer.phone` 必须允许为空。

## 3. 接口

### 3.1 微信登录

```http
POST /app/auth/wx/login
Content-Type: application/json

{
  "code": "uni.login 返回的 code"
}
```

返回示例：

```json
{
  "code": 200,
  "msg": "OK",
  "data": {
    "token": "...",
    "customerId": "123456789",
    "nickname": "微信用户1234",
    "needBindPhone": true,
    "isNewUser": true
  }
}
```

### 3.2 绑定手机号（短信验证码）

需要登录态。

```http
POST /app/auth/bind-phone
Content-Type: application/json

{
  "phone": "13800138000",
  "code": "123456"
}
```

短信验证码通过 `POST /app/auth/code?phone=13800138000` 获取。

### 3.3 绑定手机号（微信一键获取）

需要登录态。

```http
POST /app/auth/bind-phone/wx
Content-Type: application/json

{
  "wxCode": "getPhoneNumber 回调里的 code"
}
```

### 3.4 手机号账号绑定微信

需要登录态。适用于已有手机号账号、尚未绑定微信的用户。

```http
POST /app/auth/bind-wx
Content-Type: application/json

{
  "code": "uni.login 返回的 code"
}
```

### 3.5 解绑微信

需要登录态，且当前账号必须已绑定手机号，避免解绑后无法登录。

```http
POST /app/auth/unbind-wx
```

### 3.6 注销微信临时账号

需要登录态，只允许：

- 当前账号 `phone` 为空；
- 当前账号没有车牌、订单、优惠券、停车记录；
- 重新提供当前微信的 `uni.login` code 校验身份。

```http
POST /app/auth/wx/cancel
Content-Type: application/json

{
  "code": "uni.login 返回的 code",
  "confirm": true
}
```

注销后会释放 `openid`、脱敏昵称和头像、逻辑删除账号，并退出当前登录。之后可以用原手机号登录，再调用 `/app/auth/bind-wx` 绑定微信。

如果临时账号已经有业务数据，自助注销会被拒绝，需要后台处理。

## 4. 典型场景

### 老手机号用户第一次用微信登录

```text
手机号登录 → POST /app/auth/bind-wx { code }
```

### 不小心用微信登录，生成了空手机号临时账号

```text
微信登录 → 得到临时账号 B
        │
        ├─ B 没有业务数据
        │     → POST /app/auth/wx/cancel { code, confirm:true }
        │     → 手机号登录原账号 A
        │     → POST /app/auth/bind-wx { code }
        │
        └─ B 已有业务数据
              → 暂不支持自助注销，联系后台处理
```

## 5. 数据库

执行 `sql/20261024_wechat_login.sql`：

- `customer.phone` 允许为空；
- `customer.openid` 增加唯一索引，避免并发重复账号。

## 6. 说明

- 当前实现面向微信小程序，使用 `uni.login` + `jscode2session`。
- `getPhoneNumber` 必须由用户点击按钮触发，回调里拿到的是 `code`，不是手机号明文。
- `session_key` 不落库、不返回给客户端。
- 微信接口返回的 `code`、`session_key`、`access_token` 不应写入日志。
