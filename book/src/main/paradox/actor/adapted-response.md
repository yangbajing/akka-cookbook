# 适配响应类型到 actor

通常情况下，发送actor的消息类型与接收actor的响应消息类型不匹配（不然就会退化成大部分actor都继承同一个trait，这样就失去了 Typed 的意义！）。这种情况下，我们提供一个正确类型的ActorRef[T]，并将接收actor返回的响应消息T包装成发送actor可以处理的类型。

先定义一个消息包装类：

@@snip [adapter-message](../../../../../cookbook-actor/src/main/scala/cookbook/actor/requestresponse/RequestResponse.scala) { #command }

消息适配器代码：

@@snip [adapter-message](../../../../../cookbook-actor/src/main/scala/cookbook/actor/requestresponse/RequestResponse.scala) { #adapted-message }

应该为不同的消息类型注册独立的消息适配器，同一个消息类型多次注册的消息适配器只有最后一个生效。

如果响应的消息类与给定消息适配器匹配或是其消息适配器消息类型的子类型，则使用它。若有多个消息适配器符合条件，则将选用最后注册的那个。

消息适配器（`context.messageAdapter`返回的`ActorRef[T]`）的生命周期同context所在actor。建议在Behaviors.step或AbstractBehavior构造函数中注册适配器，但也可以在稍后注册它们。

注册适配器时提供的消息映射函数（`resp => WrappedBackendResponse(resp)`）在actor中运行，可安全的访问其（actor）内部状态。 但注意不能抛出异常，否则actor将被停止！

## 适用范围

1. 在不同的actor消息协议间进行转换
2. 订阅响应消息的actor，并将响应转换成发送actor可接收的类型

## 问题

1. 难以检测消息是否送达或已被处理
2. 每个响应消息只能进行一次自适应，如果注册了新的适配器则旧的将被替换。如果不同的目标actor使用相同的响应类型，则它们自动选择哪个适配器更合适。这需要在消息中编码某种相关性来解决
3. 除非协议已经包含提供上下文的方法，例如在响应中返回发送的请求ID。否则交互就不能绑定到某个上下文中。
