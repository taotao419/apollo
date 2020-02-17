# 架构和模块
## 下面是Apollo的七个模块，其中四个模块是和功能相关的核心模块，另外三个模块是辅助服务发现的模块：

1. ## ConfigService
- 提供配置获取接口
- 提供配置推送接口
- 服务于Apollo客户端

2. ## AdminService
- 提供配置管理接口
- 提供配置修改发布接口
- 服务于管理界面Portal

3. ## Client
- 为应用获取配置，支持实时更新
- 通过MetaServer获取ConfigService的服务列表
- 使用客户端软负载SLB方式调用ConfigService

4. ## Portal
- 配置管理界面
- 通过MetaServer获取AdminService的服务列表
- 使用客户端软负载SLB方式调用AdminService

## 三个辅助服务发现模块
1. ## Eureka
- 用于服务发现和注册
- Config/AdminService注册实例并定期报心跳
- 和ConfigService住在一起部署

2. ## MetaServer
- Portal通过域名访问MetaServer获取AdminService的地址列表
- Client通过域名访问MetaServer获取ConfigService的地址列表
- 相当于一个Eureka Proxy
- 逻辑角色，和ConfigService住在一起部署

3. ## NginxLB
- 和域名系统配合，协助Portal访问MetaServer获取AdminService地址列表
- 和域名系统配合，协助Client访问MetaServer获取ConfigService地址列表
- 和域名系统配合，协助用户访问Portal进行配置管理

# Apollo架构V1
## 如果不考虑分布式微服务架构中的服务发现问题，Apollo的最简架构如下图所示：
![](https://mmbiz.qpic.cn/mmbiz_png/ELH62gpbFmGdnIjxDT7AOQyZgl2KQnz6SNgVAvt0zKibxC0IqAQxvjkMibc0k8ibk1fZ0d7UGLSf96ibupPJ2jueOg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)