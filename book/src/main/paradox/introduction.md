# 介绍

Akka Typed Actor从2.4开始直到2.5可以商用，进而Akka 2.6已经把Akka Typed Actor做为推荐的Actor使用模式。Typed Actor与原先的Untyped Actor最大且直观的区别就是`ActorRef`有类型了，其签名也改成了`akka.actor.typed.ActorRef[T]`。

## HelloWorld

第一个示例是一个 Ping-Pong，从一个actor发送消息到另一个actor，并收到回复。

@@snip [Ping-Pong](../../../../cookbook-actor/src/main/scala/cookbook/actor/introduction/HelloWorld.scala) { #ping-pong }

运行actor需要有一个ActorSystem，这面的代码将执行这个示例。

@@snip [Ping-Pong](../../../../cookbook-actor/src/main/scala/cookbook/actor/introduction/HelloWorld.scala) { #helloworld }

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

Akka Typed不再需要通过类的形式来实现Actor接口定义，而是函数的形式来定义actor。可以看到，定义的actor类型为`Behavior[T]`（形为），通过`Behaviors.receiveMessage[T](T => Behavior[T]): Receive[T]`函数来处理接收到的消息，而`Receive`继承了`Behavior trait`。通过函数签名可以看到，每次接收到消息并对其处理完成后，都必须要返回一个新的形为。

`apply(): Behavior[Command]`函数签名里的范性参数类型`Command`限制了这个actor将只接收`Command`或`Command`子类型的消息，编译器将在编译期对传给actor的消息做类型检查，相对于从前的untyped actor可以向actor传入任何类型的消息，这可以限制的减少程序中的bug。特别是在程序规模很大，当你定义了成百上千个消息时。

也因为有类型的actor，在Akka Typed中没有了隐式发送的`sender: ActorRef`，必须在发送的消息里面包含回复字段，就如`PingCommand`消息定义里的`replyTo: ActorRef[Ping.Command]`字段一样。actor在处理完消息后可以通过它向发送者回复处理结果。

## ActorRef

`ActorRef[T]`是`Behavior[T]`被ActorSystem构造后创建的actor的引用，与经典`ActorRef`的区别显而易见，它拥有了一个类型参数`T`，`T`限定了这个actor只能处理`T`或它的子类型。这相对经典actor是一大进步，特别是在你的actor系统规模很大时，若没有一个静态的类型约束你将十之八九会迷失在消息的海洋……

## 构造 ActorSystem

`ActorSystem`构造至少需要转入两个参数：

- `guardianBehavior: Behavior[T]`：守卫行为，它将被创建为守卫actor，其 ActorPath 地址为：`akka://helloworld/user`。
- `name: String`：ActorSystem名字，这个名字除了在日志线程中显示外，在Akka Cluster时也很重要，用于标识同一个集群。
