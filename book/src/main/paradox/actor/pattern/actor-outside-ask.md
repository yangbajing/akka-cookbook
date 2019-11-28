# actor 外使用 ask（请求-响应模式）

在与非Akka应用集成时，通常需要向actor发送一个请求并期待它有响应。这可以通过`ask`函数（或`!`）来实现。

```scala
import akka.actor.typed.scaladsl.AskPattern._
import scala.concurrent.duration._

implicit val system: ActorSystem[_] = system
implicit val timeout: Timeout = 3.seconds

val result: Future[Hello.Response] = 
  cookieFabric.ask(ref => Hello.SayHello("Scala", ref))
```

@@@note
`import AskPattern._`导入的`ask`函数本来需要有一个`Scheduler`的隐式参数，但`object AskPattern`还同时提供了一个`schedulerFromActorSystem`隐式函数从`ActorSystem[_]`获得`Scheduler`，这里建议直接使用`implicit ActorSystem[_]`（在使用Akka Streams时，也提供了从`ActorSystem[_]`获得`Materializer`的隐式转换函数，直接使用`implicit ActorSystem[_]`可以减少样版代码，使代码更清晰）。
@@@

## 适用范围

1. 从actor系统外部访问时，如Akka HTTP请求访问actor获取响应值

## 问题

1. 在返回的Future回调内很可能意外的捕获了外部状态，因为这些回调将在与ask不同的线程上执行
2. 一个ask只能有一个响应（ask将生成临时actor）
3. 当请求超时时，接收actor并不知道且仍将继续处理请求直至完成，甚至可能会在超时发生后才开始处理它
