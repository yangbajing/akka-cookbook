# OAuth 2 Service

实现一个 OAuth 2 服务有几个核心点：

1. OAuth 2 协议解析
2. 连接的用户可能很多，系统需支持横向扩展
3. 每个连接用户的 `access_token` 的状态控制：有效期控制
4. 服务要支持容错、可恢复、可扩展、高并发等特性

使用 Akka 来实现 OAuth 2 服务会发现逻辑非常的清晰，且能很好的实现以上几个核心点。

每个连接用户可抽像为一个 **Actor**，这样多个连接用户即可并发访问。

使用 **akka-cluster-sharding** 我们可以实现连接用户的集群部署、横向扩展。

而 **akka-persistence** 提供 `EventSourcedBehavior` 为 **Actor** 添加了持久化能力，这实现了可恢复特性。

Akka Actor提供了监管机制，这样我们可对错误快速响应，实现了容错性。

## `access_token` management

在 Akka 中通过 Actor 模型来设计 access_token 有两种主要方案：

1. 每个 access_token 一个 Actor，通过 ClusterSharding 来水平扩展，将 Akka Actor 做为一种有状态的缓存来使用。
0. 每个用户（User）一个 Actor，在用户 Actor 内部通过状态来保存多个 access_token 。

### Per access_token one Actor

每个 access_token 一个 Actor 在设计上比较简单，只需要注意在过期时间到时停止此 Actor。示例代码如下：

@@snip [AccessTokenEntity](../../scala/book/oauth2/peraccesstoken/AccessTokenEntity.scala) { #AccessTokenEntity }

这种方案的好处在于：

1. 通过 ClusterSharding 可以实现理论上无限水平扩展的集群，无论多少个 access_token 都可以保存下来
0. 容错、可恢复，节点挂掉后可从其它机器上恢复令牌状态
0. 生成的 access_token 令牌不需要含有业务信息，只需要保证唯一性即可
0. 代码逻辑直观

这种方案的缺点有：

1. 每个 access_token 一个 Actor，Actor 所做的功能不多，相对具有过期时间（TTL）的缓存数据存储系统来说优势不明显
0. 每个无效 access_token 都会在生成一个 Actor 后才可以判断是否有效，这会造成创建很多无效的 Actor

### Per user one Actor

@@snip [UserEntity](../../scala/book/oauth2/peruser/UserEntity.scala) { #UserEntity }

每个用户一个 Actor 的优势有：

1. 通过 ClusterSharding 可以实现理论上无限水平扩展的集群，无论多少个 access_token 都可以保存下来
0. 容错、可恢复，节点挂掉后可从其它机器上恢复令牌状态
0. 相比每 access_token 一个 Actor，此方案可以显著减少系统 Actor 的数量
0. 相比拥有过期时间（TTL）的缓存数据存储系统，使用 Actor 更灵活，且可于业务（用户）系统在同一集群
0. 无效 access_token 不会生成多于的 Actor

每个用户一个 Actor 的缺点有：

1. 生成的 access_token 令牌不需要含有业务信息，如：用户ID
0. 从代码行数上可看出，相对每 access_token 一个 Actor ，此方案代码逻辑相对复杂，但功能更加强大！
