# 集群限流

限流的主要目的是对并发访问进行速度控制，在很多业务场景中都会用到。如：防止突发流量打垮服务、控制客户端调用某些 API 的次数和防止爬虫等。

常见的限流工具有：

1. `RateLimiter`（来自 Guava 库）：基于令牌桶算法实现的单机限流
2. Nginx：提供了多种限流算法模块
3. `Sentinel`：由阿里巴巴开发的一款分布式限流框架。

本文开始将通过使用 Guava 的 `RateLimiter` 和 Akka 快速实现一个集群限流功能，然后再根据可能的实际使用情况基于 Akka 实现一个限流库/框架，并讨论其在 Akka 或非 Akka 环境中的各种适用场景及用法。 

## 极速实现

这里通过 Guava 的 `RateLimiter` + Akka Cluster Sharding 快速实现一个集群限流器，示例代码如下：

@@snip [ClusterRate](../../../../../cookbook-cluster/src/main/scala/cookbook/rate/ClusterRate.scala) { #ClusterRate }

这就是在 Akka 中实现一个集群限流功能的所有代码了，我们来分析下它实现的功能：

1. 我们通过使用 Akka Cluster Sharding 来实现了一个基于“分片机制”的分布式 `Entity` actor，其 `entityId`（即：`entityContext.entityId`）既为分片 ID，Akka 可以保证在相同时间只有一个 `Entity` actor 在集群内存活。这样，我们就可以通过 Akka 的 `Entity` actor 获得了一个集群内唯一的 `RateLimiter`，而 `entityId` 就可以用来区分不同地限流简略（或者对应到不同的限流 URI、服务名等）。
2.  在 `ClusterRate` 伴身对象里定义了两个消息和一个返回值，实现：
    - 申请令牌（`Acquire`为请求，`Acquired`为响应）
    - 重置每秒可生成令牌数（`SetRate`）

### 测试

测试非常简单，我们将发起 3 次 `Acquire` 请求申请令牌，然后观察断言结果。

@@snip [ClusterRateTest](../../../../../cookbook-cluster/src/test/scala/cookbook/rate/ClusterRateTest.scala) { #ClusterRateTest }

上面是对 `ClusterRate` 的一个粗略的单元测试，我们可以看到 `acquireResult2` 的结果为 `false`，而不是预期的 `true`，这是因为 guava `RateLimiter` 里设置的每秒令牌数并不是一次性生成的，它会在 1 秒内均匀分配。

### 问题

Akka 下一个集群限流功能实现好了，但它还是有些的问题；或者，你不知道在 Akka 环境或非 Akka 环境中怎样使用它？这些问题有：

1. `Acquire` 不支持超时
2. Akka Actor 中怎样此集群限流功能？
3. Akka HTTP 中怎样此集群限流功能？
4. 非 Akka 环境中可以使用此功能吗？比如：Spring

在接下来的系列文章中将对其进行一一解决，敬请期待。

## 超时等待

TODO 1. actor 内不应调用阻塞函数 .tryAcquire X
     2. 重新实现 RateLimiter，通过 Akka timeout 垂柳 .tryAcquire

TODO 1. acquireId 区分每次令牌获取
     2. RateLimiter 未限制突发流量问题的解决
