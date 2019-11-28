# 测试 actor

`akka-actor-testkit-typed`测试库为Akka（Typed）提供了开箱即用的测试工具，通常可以实现`ScalaTestWithActorTestKit`来实现自己的单元测试。有关更多 **scalatest** 的内容请访问官方网站： [http://scalatest.org](http://www.scalatest.org/)。

要使用`akka-actor-testkit-typed`对Akka代码进行测试，需要添加以下依赖到sbt项目：

@@dependency [sbt,Maven,Gradle] { group=com.typesafe.akka artifact=akka-actor-testkit-typed_$scala.binary.version$ version=$akka.version$ }

## 测试代码

首先列出待测试actor的消息协议：

@@snip [TestActorSpec.scala](../../../../../cookbook-actor/src/test/scala/cookbook/actor/test/TestActorSpec.scala) { #TestActor_messages }

下面是待测试actor的实现：

@@snip [TestActorSpec.scala](../../../../../cookbook-actor/src/test/scala/cookbook/actor/test/TestActorSpec.scala) { #TestActor_apply }

## 使用 TestProbe 预期答复

@@snip [TestActorSpec.scala](../../../../../cookbook-actor/src/test/scala/cookbook/actor/test/TestActorSpec.scala) { #Req-Resp-probe }

### 创建 actor

`ScalaTestWithActorTestKit`提供了`spawn`函数来创建actor，它提供了几个重载版本以满足不同的需求，比如：创建一个匿名actor。

通过`createTestProbe`可以创建一个接收指定消息类型的测试探测（`TestProbe[Reply]`），这可用于作为消息内的回复actor（通过`probe.ref`返回一个`ActorRef[Reply]`）。

### 断言消息

同时`TestProbe`提供了一系统断言函数用于执行测试，如：`expectMessage`、`expectNoMessage`、`expectMessageType`和`fishForMessage`等函数。

通过`probe.ref`接收到的消息将被`TestProbe`缓存下来，待每次调用`expectXXX`系列函数时被使用，而`expectXXX`系列函数将阻塞调用进程，这可以以发送消息时一样的顺序对响应进行测试。

### 断言消息的缺失

当所有已发送消息都收到预期回复后，使用`probe.expectNoMessage(2.seconds)`来确认没有更多回复将被收到。

### expect 函数超时设置

通过覆盖`def remainingOrDefault: FiniteDuration`函数可以设置`expectXXX`系列函数的默认超时时间，也可以配置HOCON参数`akka.actor.testkit.typed.single-expect-default`来修改它：
```hocon
akka.actor.testkit.typed.single-expect-default = 10.seconds
```

## 使用 ask 

@@snip [TestActorSpec.scala](../../../../../cookbook-actor/src/test/scala/cookbook/actor/test/TestActorSpec.scala) { #Req-Resp-ask }

`ask`接受一个函数，函数参数为构造的一个临时actor `ActorRef[Reply]`（通过`ask[T]`指定临时actor的参数类型），返回值是发送给被调用actor的消息。`ask`模式发送的消息将返回一个`Future[T]`，通过调用`futureValue`将阻塞等待这个`Future`执行完成并获得结果。

### futureValue超时设置

`futureValue`默认超时时间可以通过`PatienceConfig`配置，它由`ScalaFutures`特质引入，而`ScalaTestWithActorTestKit`已经with了此特质。可通过覆盖`patience`隐式参数设置`futureValue`调用的超时时间：
```scala
override implicit val patience = 
  PatienceConfig(Span(5, Seconds), Span(10, Milliseconds))
```

`PatienceConfig`的第一个参数`timeout`配置调用的超时总时间，第二个参数`interval`配置两次检测`Future`是否完成的间隔时间。

## 异步断言

### 使用 AsyncWordSpec 特质

使用`AsyncWordSpec`进行异步断言测试，它需要每个测试用例都以`Future`返回，我们在`Future`的转换函数里进行断言测试：

@@snip [TestActorAsyncSpec](../../../../../cookbook-actor/src/test/scala/cookbook/actor/test/TestActorAsyncSpec.scala) { #TestActorAsyncSpec }

@@@note
读者会发现，这里使用了`AsyncWordSpecLike`特质而非`AsyncWordSpec`，包括`TestActorSpec`也使用了以`Like`后缀结尾的特质。加`Like`后缀和未加的区别在哪？其实看下两者的签名即可知道，有`Like`后缀的是以`trait`定义的，而没有的却是用`class`或`abstract class`定义的。因为Java不允许多继承（Scala兼容Java），而`ScalaTestWithActorTestKit`本身是一个抽像类，所以我们需要使用各测试风格规范（如：`WordSpec`、`FunSpec`、`FlatSpec`、`AsyncWordSpec`等）`Like`后缀版本。
@@@
