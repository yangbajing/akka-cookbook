# 测试actor（scalatest）

`akka-actor-testkit-typed`测试库为Akka（Typed）提供了开箱即用的测试工具，通常可以实现`ScalaTestWithActorTestKit`来实现自己的单元测试。有关更多 **scalatest** 的内容请访问官方网站： [http://scalatest.org](http://www.scalatest.org/)。

首先列出待测试actor的消息协议：

@@snip [TestActorSpec.scala](../../../../../cookbook-actor/src/test/scala/cookbook/actor/test/TestActorSpec.scala) { #TestActor_messages }

下面是待测试actor的实现：

@@snip [TestActorSpec.scala](../../../../../cookbook-actor/src/test/scala/cookbook/actor/test/TestActorSpec.scala) { #TestActor_apply }

## 使用 TestProbe 测试响应

`ScalaTestWithActorTestKit`提供了`spawn`函数来创建actor，它提供了几个重载版本以满足不同的需求，比如：创建一个匿名actor。

@@snip [TestActorSpec.scala](../../../../../cookbook-actor/src/test/scala/cookbook/actor/test/TestActorSpec.scala) { #Req-Resp-probe }

通过`createTestProbe`可以创建一个接收指定消息类型的测试探测（`TestProbe[Reply]`），这可用于作为消息内的回复actor（通过`probe.ref`返回一个`ActorRef[Reply]`）。同时`TestProbe`提供了一系统断言函数用于执行测试，如：`expectMessage`、`expectNoMessage`、`expectMessageType`和`fishForMessage`等函数。

通过`probe.ref`接收到的消息将被`TestProbe`缓存下来，待每次调用`expectXXX`系列函数时被使用。

## 使用 ask 测试响应 

@@snip [TestActorSpec.scala](../../../../../cookbook-actor/src/test/scala/cookbook/actor/test/TestActorSpec.scala) { #Req-Resp-ask }

`ask`接受一个函数，函数参数为构造的一个临时actor `ActorRef[Reply]`（通过`ask[T]`指定临时actor的参数类型），返回值是发送给被调用actor的消息。`ask`模式发送的消息将返回一个`Future[T]`，通过调用`futureValue`将阻塞等待这个`Future`执行完成并获得结果。

`futureValue`默认超时时间可以通过`PatienceConfig`配置，它由`ScalaFutures`特质引入，而`ScalaTestWithActorTestKit`已经with了此特质。可通过覆盖`patience`隐式参数设置`futureValue`调用的超时时间：
```scala
override implicit val patience = 
  PatienceConfig(Span(5, Seconds), Span(10, Milliseconds))
```