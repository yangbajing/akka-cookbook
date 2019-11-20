# 介绍

## HelloWorld

第一个示例是一个 Ping-Pong，从一个actor发送消息到另一个actor，并收到回复。

@@snip [Ping-Pong](../../../../../cookbook-actor/src/main/scala/cookbook/actor/introduction/HelloWorld.scala) { #ping-pong }

运行actor需要有一个ActorSystem，这面的代码将执行这个示例。

@@snip [Ping-Pong](../../../../../cookbook-actor/src/main/scala/cookbook/actor/introduction/HelloWorld.scala) { #helloworld }

运行示例程序，可看到如何输出：

```sbtshell
sbt:akka-cookbook> cookbook-actor/runMain cookbook.actor.introduction.HelloWorld
[info] running (fork) cookbook.actor.introduction.HelloWorld 
[2019-11-19 19:24:39,285] [INFO] [akka.event.slf4j.Slf4jLogger] [helloworld-akka.actor.default-dispatcher-3] [] - Slf4jLogger started
[2019-11-19 19:24:39,374] [INFO] [cookbook.actor.introduction.Ping$] [helloworld-akka.actor.default-dispatcher-3] [akka://helloworld/user] - Started Pong actor and send message complete.
[2019-11-19 19:24:39,374] [INFO] [cookbook.actor.introduction.Pong$] [helloworld-akka.actor.default-dispatcher-6] [akka://helloworld/user/pong] - Receive ping message: Scala
[2019-11-19 19:24:39,375] [INFO] [cookbook.actor.introduction.Ping$] [helloworld-akka.actor.default-dispatcher-3] [akka://helloworld/user] - Receive pong message: Hello Scala
[success] Total time: 1 s, completed Nov 19, 2019 19:24:39 PM
```

## Behavior

Akka Typed使用`Behavior[T]`（ **行为** ）替代了经典（Untyped）actor的`Actor`特质。每次actor被调用后都需要返回一个新的行为以待下一次actor被执行时调用。`Behavior`更明确的表达出actor是由一系列响应消息的行为组成，它可以是一个纯函数（ *就像示例一样，Behavior由函数创建，它关不需要定义一个类* ），也可以拥有状态……

## ActorRef

`ActorRef[T]`是`Behavior[T]`被ActorSystem构造后创建的actor的引用，与经典`ActorRef`的区别显而易见，它拥有了一个类型参数`T`，`T`限定了这个actor只能处理`T`或它的子类型。这相对经典actor是一大进步，特别是在你的actor系统规模很大时，若没有一个静态的类型约束你将十之八九会迷失在消息的海洋……

## 构造 ActorSystem

`ActorSystem`构造至少需要转入两个参数：

- `guardianBehavior: Behavior[T]`：守卫行为，它将被创建为守卫actor，其 ActorPath 地址为：`akka://helloworld/user`。
- `name: String`：ActorSystem名字，这个名字除了在日志线程中显示外，在Akka Cluster时也很重要，用于标识同一个集群。
