# VM 重启后 Nacos 连接失败 — Docker Hairpin NAT 修复

## 环境信息

| 项目 | 信息 |
|------|------|
| VM 系统 | CentOS 7 |
| Docker 网络 | bridge 模式，网段 172.30.0.0/16、172.28.0.0/16、172.17.0.0/16 |
| Nacos 地址 | 192.168.150.101:8848 |
| Nacos 容器 IP | 172.30.0.5 |

## 问题现象

虚拟机重启后，从宿主机或本机 Java 应用无法连接 Nacos（192.168.150.101:8848），表现为连接超时。但：
- Nacos 容器正常运行（`docker ps` 状态 UP）
- VM 内部 `curl 127.0.0.1:8848` 正常返回 OK
- **VM 内部 `curl 192.168.150.101:8848` 同样超时**

## 根因分析

**Docker Hairpin NAT 不对称路由问题。**

### 正常流量路径（外部访问）

```
宿主机/本机 → 192.168.150.101:8848 → docker-proxy → DNAT → 容器 172.30.0.5:8848
```

### 问题流量路径（VM 自身访问自身公网 IP）

```
VM → 192.168.150.101:8848
    → docker-proxy (DNAT to 172.30.0.5:8848)
    → 容器接收，源 IP = VM 网卡 IP (如 192.168.150.101)
    → 容器回包直接走 Docker bridge，源 IP = 172.30.0.5
    → 客户端收到来自 172.30.0.5 的响应，但期望来自 192.168.150.101
    → TCP 协议栈丢弃该包 → 连接超时
```

关键矛盾：**请求走 docker-proxy 进入容器，但回包绕过 docker-proxy 直接走 bridge 返回**。客户端的 TCP 栈认为这是非法回包（源 IP 不匹配），直接丢弃。

### 为什么重启后出现

iptables 规则存储在内存中，VM 重启后全部清空。Docker 启动时会自动添加 DNAT 规则（用于端口映射），但 **不会添加 POSTROUTING MASQUERADE 规则**（用于 hairpin 场景），因此重启后问题必现。

## 解决方案

在 iptables NAT 表的 POSTROUTING 链最前面插入 MASQUERADE 规则，让同一 Docker 网段内的容器间流量经过 SNAT，确保回包路径与请求路径一致。

```bash
iptables -t nat -I POSTROUTING 1 -s 172.30.0.0/16 -d 172.30.0.0/16 -j MASQUERADE
iptables -t nat -I POSTROUTING 1 -s 172.28.0.0/16 -d 172.28.0.0/16 -j MASQUERADE
iptables -t nat -I POSTROUTING 1 -s 172.17.0.0/16 -d 172.17.0.0/16 -j MASQUERADE
```

修复后的流量路径：

```
VM → 192.168.150.101:8848
    → docker-proxy (DNAT to 172.30.0.5:8848)
    → 容器接收，源 IP = 172.30.0.1 (bridge IP，而非 192.168.150.101)
    → 容器回包到 172.30.0.1 (bridge gateway)
    → docker-proxy 收到回包，SNAT 还原 → 源 IP = 192.168.150.101
    → 客户端收到正确源 IP 的响应 → 连接成功
```

## 修复步骤

### 1. SSH 登录 VM

```bash
ssh root@192.168.150.101
# 密码: 123321
```

### 2. 立即生效 — 添加 iptables 规则

```bash
# 检查规则是否已存在
iptables -t nat -C POSTROUTING -s 172.30.0.0/16 -d 172.30.0.0/16 -j MASQUERADE 2>/dev/null && echo "已存在" || echo "不存在"

# 添加规则（排除检查，不存在则添加）
iptables -t nat -C POSTROUTING -s 172.30.0.0/16 -d 172.30.0.0/16 -j MASQUERADE 2>/dev/null || \
    iptables -t nat -I POSTROUTING 1 -s 172.30.0.0/16 -d 172.30.0.0/16 -j MASQUERADE

iptables -t nat -C POSTROUTING -s 172.28.0.0/16 -d 172.28.0.0/16 -j MASQUERADE 2>/dev/null || \
    iptables -t nat -I POSTROUTING 1 -s 172.28.0.0/16 -d 172.28.0.0/16 -j MASQUERADE

iptables -t nat -C POSTROUTING -s 172.17.0.0/16 -d 172.17.0.0/16 -j MASQUERADE 2>/dev/null || \
    iptables -t nat -I POSTROUTING 1 -s 172.17.0.0/16 -d 172.17.0.0/16 -j MASQUERADE
```

### 3. 验证规则生效

```bash
# 查看 POSTROUTING 链
iptables -t nat -L POSTROUTING -n | grep MASQUERADE

# 从 VM 自身测试 Nacos
curl -s --connect-timeout 5 http://192.168.150.101:8848/nacos/v1/console/health/readiness
# 预期输出: OK
```

### 4. 持久化 — 写入 rc.local（重启后自动加载）

编辑 `/etc/rc.d/rc.local`，在 `touch /var/lock/subsys/local` 之前添加：

```bash
# Docker hairpin NAT fix - must run after docker starts
sleep 10
iptables -t nat -C POSTROUTING -s 172.30.0.0/16 -d 172.30.0.0/16 -j MASQUERADE 2>/dev/null || iptables -t nat -I POSTROUTING 1 -s 172.30.0.0/16 -d 172.30.0.0/16 -j MASQUERADE
iptables -t nat -C POSTROUTING -s 172.28.0.0/16 -d 172.28.0.0/16 -j MASQUERADE 2>/dev/null || iptables -t nat -I POSTROUTING 1 -s 172.28.0.0/16 -d 172.28.0.0/16 -j MASQUERADE
iptables -t nat -C POSTROUTING -s 172.17.0.0/16 -d 172.17.0.0/16 -j MASQUERADE 2>/dev/null || iptables -t nat -I POSTROUTING 1 -s 172.17.0.0/16 -d 172.17.0.0/16 -j MASQUERADE
```

确保 rc.local 有执行权限：

```bash
chmod +x /etc/rc.d/rc.local
```

### 5. 验证持久化

```bash
# 查看 rc.local 内容
cat /etc/rc.d/rc.local

# 手动执行测试
bash /etc/rc.d/rc.local && echo "rc.local 执行成功"
```

### 6. 重启容器使当前环境生效

```bash
cd /usr/local/src
docker-compose restart
```

## 验证清单

| 检查项 | 命令 | 预期结果 |
|--------|------|----------|
| Nacos 健康检查 | `curl http://192.168.150.101:8848/nacos/v1/console/health/readiness` | `OK` |
| Docker 容器状态 | `docker ps` | 所有容器 UP |
| MASQUERADE 规则 | `iptables -t nat -L POSTROUTING -n \| grep MASQUERADE` | 包含 3 条 `172.x.x.x/16` 规则 |
| rc.local 可执行 | `ls -l /etc/rc.d/rc.local` | `-rwxr-xr-x` |

## 注意事项

1. 如果以后新增了 Docker 自定义网络（新 bridge 网段），需要同步添加对应的 MASQUERADE 规则
2. `rc.local` 中的 `sleep 10` 是必要的——确保 Docker daemon 完全启动后再添加规则
3. 如果 VM 重装系统或重置 iptables，需要重新执行步骤 2 和步骤 4
