# actor之间使用ask（请求-响应模式）

当请求与响应之间存在1:1映射时，可以通过调用`ActorContext[T]`上的`ask`函数来与另一个actor进行交互。

构造一个传出消息，它使用`context.ask[Response]`提供的`ActorRef[Response]`作为接收响应的actor放入消息中将成功/失败（`Try[Response]`）转换为发送者actor可接收的消息类型

@@snip [PingPongMain](../../../../../cookbook-actor/src/main/scala/cookbook/actor/pingpong/PingPongMain.scala) { #ping }

`context.ask`的响应映射函数在接收actor中运行，可以安全的访问actor内部状态， **但抛出异常的话actor将会被停止** 。

```scala
  override def ask[Req, Res](target: RecipientRef[Req], createRequest: ActorRef[Res] => Req)(
      mapResponse: Try[Res] => T)(implicit responseTimeout: Timeout, classTag: ClassTag[Res]): Unit = {
    import akka.actor.typed.scaladsl.AskPattern._
    pipeToSelf((target.ask(createRequest))(responseTimeout, system.scheduler))(mapResponse)
  }

  def pipeToSelf[Value](future: Future[Value])(mapResult: Try[Value] => T): Unit = {
    future.onComplete(value => self.unsafeUpcast ! AdaptMessage(value, mapResult))
  }
```

上面是`context.ask`函数实现：

- `target`：接收actor引用
- `createRequest`：创建请求消息函数，参数是ask创建的临时actor，此临时actor用于适配接收actor的消息类型
- `mapResponse`：将获取的响应消息类型Res映射成请求actor可以接收的消息类型

可以看到，`context.ask`函数实际上是在目标actor（`target`）上调用了`ask`方法，并将返回的`Future[T]`结果转换并发送到`context`所在的actor。

## 适用范围

1. 单个查询响应的转换
2. 发送actor需要在继续之前知道消息已被处理（通过`context.ask(..., ...)(mapResponse)`的`mapResponse`函数）
3. 如果请求超时，允许actor重新发送消息（通过`mapResponse`回调函数里处理）
4. 跟踪未完成的请求
5. 保存上下文。发送者actor接收的请求有上下文信息（context.ask将生成一个临时actor，这个临时actor即可作为一个确定上下文的载体），如：请求ID reqId，而后端协议不支持这个参数时

## 问题

1. 一个ask只能有一个响应（因为ask会创建一个临时actor，这个actor在收到响应后就会结束自己）
2. 当请求超时时，接收actor（发回响应的那个）并不知道且仍可能将请求处理并完成，甚至若接收actor很忙的话会在请求超时发生以后再处理它
3. 为超时情况找到一个好的（包装）值，特别是在ask函数调用后还会触发链式调用时（一个异步调用完成后进行另一个异步调用）。这时候希望来快速响应超时情况并回复请求者，但同时需要避免误报。

## 完整代码

@@snip [PingPongMain](../../../../../cookbook-actor/src/main/scala/cookbook/actor/pingpong/PingPongMain.scala) 
