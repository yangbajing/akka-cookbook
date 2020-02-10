# 将 Future 结果发送给（actor）自己

当在actor内部执行异步操作（返回一个`Future`时）需要小心处理，因为actor与那个异步操作不在同一个线程。`ActorContext[T]`提供了`pipeToSelf`方法来将`Future`的结果安全的传给自己（actor）。

在`Future`的`onComplete`回调函数里处理异步结果很方便、看起来也很诱人，但这样会引发很多潜在的危险，因为从外部线程访问actor内部状态不是线程安全的。例如：无法从类似回调中线程安全的访问示例的`operationsInProgress`计数器，所以，最好将响应映射到消息，并使用actor的消息接收机制来线程安全的执行进一步处理。

```scala
case Update(value, replyTo) =>
  if (operationsInProgress == MaxOperationsInProgress) {
    // ....
    Behaviors.same
  } else {
    val futureResult = dataAccess.update(value)
    context.pipeToSelf(futureResult) {
      case Success(_) => WrappedUpdateResult(UpdateSuccess(value.id), replyTo)
      case Failure(e) => WrappedUpdateResult(UpdateFailure(value.id, e.getMessage), replyTo)
    }
    next(dataAccess, operationsInProgress + 1)
  }
case WrappedUpdateResult(result, replyTo) =>
  replyTo ! result
  next(dataAccess, operationsInProgress - 1)
```

@@@warning
`context.pipeToSelf`函数签名如下：

```scala
def pipeToSelf[Value](future: Future[Value])(mapResult: Try[Value] => T): Unit
```

需要注意的是在`mapResult`函数中不能抛出异常，不让`actor`将被停止！
@@@

## 适用范围

1. 调用返回`Future`的外部服务时
2. 当`Future`完成，actor需要继续处理时
3. 保留原始请求的上下文，并在`Future`完成时使用它。如：`replyTo: ActorRef[_]`

## 问题

1. 为`Future`结果添加过多的包装消息
