# 创建 Source 的常用函数

## `fromPublisher`
```scala
Source.fromPublisher[T](publisher: Publisher[T]): Source[T, NotUsed]
```

从 Reactive Streams 的`Publisher[T]`接口生成一个`Source`，当于实现了 Reactive Streams 协议规范的流处理库、框架互进行操作时非常有用。

## `asSubscriber`
```scala
Source.asSubscriber[T]: Source[T, Subscriber[T]]
```

创建一个新的`Source`，它的物化值为`Subscriber[T]`，这样你可以调用`Subscriber`上的API来消费上游发送的数据。

## `fromIterator`
```scala
Source.fromIterator[T](f: () => Iterator[T]): Source[T, NotUsed]
```

从一个`Iterator` **生成迭代器** 创建`Source`，只有当下游调用`pull`函数请求时，上游（`Source`）才会生产数据，这里既才会调用`Iterator`的`next`方法生成整数。若迭代器不为空，则`Source`将一真可生成数据。如：`Source.fromIterator(() => Iterator.from(0))`，它将创建一个从数字`0`开始的无限源（`Source`）。

## `cycle`
```scala
Source.cycle[T](f: () => Iterator[T]): Source[T, NotUsed]
```

根据迭代器循环生成元素，下游可无限请求`Source`生成数据。

## `fromGraph`
```scala
Source.fromGraph[T, M](g: Graph[SourceShape[T], M]): Source[T, M]
```

从一个`SourceShape`图生成`Source`，当需要自定义实现一个`Source`时需要通过此函数将图转换成可使用的`Source`。

## `apply`
```scala
Source.apply[T](iterable: immutable.Iterable[T]): Source[T, NotUsed]
```

从一个有限可迭代的集合生成一个`Source`。

## `queue`
```scala
Source.queue[T](bufferSize: Int, overflowStrategy: OverflowStrategy):
    Source[T, SourceQueueWithComplete[T]]
```

- `bufferSize`：设置队列最大可保存数据（若下游一直不`pull`数据）
- `overflowStrategy`：当队列满时的溢出策略

创建一个队列源，它的物化值是一个队列，可通过此队列向`Source`传入指定类型的数据。

*示例代码：*

@@snip [Queue Source](../../../../../cookbook-streams/src/test/scala/cookbook/streams/SourceTest.scala) { #queue }

- `Source.tick[T](initialDelay: FiniteDuration, interval: FiniteDuration, tick: T): Source[T, Cancellable]`：
- `Source.single[T](element: T): Source[T, NotUsed]`：
- `Source.lazySingle[T](create: () => T): Source[T, NotUsed]`：
- `Source.unfold[S, E](s: S)(f: S => Option[(S, E)]): Source[E, NotUsed]`：
- `Source.unfoldAsync[S, E](s: S)(f: S => Future[Option[(S, E)]]): Source[E, NotUsed]`：
- `Source.unfoldResource[T, S](create: () => S, read: (S) => Option[T], close: (S) => Unit): Source[T, NotUsed]`：
- `unfoldResourceAsync[T, S](
         create: () => Future[S],
         read: (S) => Future[Option[T]],
         close: (S) => Future[Done]): Source[T, NotUsed]`：
- `Source.repeat[T](element: T): Source[T, NotUsed]`：`repeat`由`unfold`函数实现
- `Source.maybe[T]: Source[T, Promise[Option[T]]]`：
- `Source.future[T](futureElement: Future[T]): Source[T, NotUsed]`：
- `Source.lazyFuture[T](create: () => Future[T]): Source[T, NotUsed]`：
- `Source.futureSource[T, M](futureSource: Future[Source[T, M]]): Source[T, Future[M]]`：
- `Source.lazyFutureSource[T, M](create: () => Future[Source[T, M]]): Source[T, Future[M]]`：
- `Source.combine[T, U](first: Source[T, _], second: Source[T, _], rest: Source[T, _]*)(
                strategy: Int => Graph[UniformFanInShape[T, U], NotUsed]): Source[U, NotUsed]`：
- `Source.zipN[T](sources: immutable.Seq[Source[T, _]]): Source[immutable.Seq[T], NotUsed]`：
- `Source.zipWithN[T, O](zipper: immutable.Seq[T] => O)(sources: immutable.Seq[Source[T, _]]): Source[O, NotUsed]`：
- `Source.actorRef[T](
            completionMatcher: PartialFunction[Any, CompletionStrategy],
            failureMatcher: PartialFunction[Any, Throwable],
            bufferSize: Int,
            overflowStrategy: OverflowStrategy): Source[T, ActorRef]`：

