# 深入 Source

## Source

`Source`是一组流（Streams）处理步骤，有一个打开的输出端口。`Source`可以包含任意数量已连接的内部源（`Source`）和转换操作（`Flow`）。`Source`可以通过`asPublisher`函数转换为 Reactive Streams 协议等价的`Publisher`。

```scala
final class Source[+Out, +Mat](
    override val traversalBuilder: LinearTraversalBuilder,
    override val shape: SourceShape[Out])
    extends FlowOpsMat[Out, Mat]
    with Graph[SourceShape[Out], Mat]
```

`Source`的类签名有两个类型参数，它们都是协变的。

- `Out`：输出元素类型
- `Mat`：物化值类型，物化值可用于记录`Source`的内部状态或操作记录等。比如：`FileIO.fromPath`这个`Source`的物化值记录了从文件里实际读取到的字符数。

`Source`通过类构造器实现了`Graph`接口的`traversalBuilder`和`shape`两个参数，其中`shape`限制了必须为一个`SourceShape[Out]`类型。

`Source`还实现了`FlowOpsMat`特质，使得`Source`具有了一系列的`via`、`to`（及它们的变体）函数和丰富和流程转换函数（Flow操作符）。

- `via`：用于连接`Flow`，它将一个流处理过程与当前`Source`连接，并返回另一个`Source`，其中`Flow`的输出端口将作为新`Source`的输出端口
- `viaMat`：相对`via`多了第二个curry参数，`combine`指定保留哪边的物化值。`via`实际上相当于：`viaMat(....)(Keep.left)`
    ```scala
    def viaMat[T, Mat2, Mat3](flow: Graph[FlowShape[Out, T], Mat2])(
          combine: (Mat, Mat2) => Mat3): Source[T, Mat3]
    ```
- `to`：用于连接`Sink`，`Sink`将从上游发送的元素都聚合到一起并处理。当一个`Source`连接了`Sink`后，即形成了一个已闭合的可运行图（ **RunnableGraph** ），我们可以调用`RunnableGraph`的`run`函数来实际运行它。
- `toMat`：相对`to`多了第二个curry参数，`combine`指定保留哪边的物化值。`to`实际上相当于：`toMat(....)(Keep.left)`
    ```scala
    toMat[Mat2, Mat3](sink: Graph[SinkShape[Out], Mat2])(
          combine: (Mat, Mat2) => Mat3): RunnableGraph[Mat3]
    ```

有关`combine`函数的更多内容请见： @ref[物化与物化值#Keep](materialize.md#Keep)

通常我们都不会直接构造`Source`，而是通过`Source`的伴身对象提供了各类工具函数来创建。有关`Source`伴身对象的常用工具函数请参阅： @ref[ 创建 Source 的常用函数](source.md)。

## SourceShape

```scala
final case class SourceShape[+T](out: Outlet[T @uncheckedVariance]) extends Shape {
  override val inlets: immutable.Seq[Inlet[_]] = EmptyImmutableSeq
  override val outlets: immutable.Seq[Outlet[_]] = out :: Nil

  override def deepCopy(): SourceShape[T] = SourceShape(out.carbonCopy())
}
```

`SourceShape`使用了`final`做修饰，这就确定了`Source`的形状（`shape`）只能为`SourceShape`，而且限制为没有输入端口，只有一个输出端口的形状；唯一可定义的地方就是它的输出端口发送出数据的类型。

## Source 怎么生产数据？

`Source`是怎么生产数据并发送到下游的呢？是在`GraphStageLogic`里调用`push`函数将数据推送到下游的。`GraphStageLogic`用于定义图处理实际逻辑，它需要通过`GraphStageWithMaterializedValue`抽像类提供的方法创建，而这人抽像类继承了`Graph`特质。

通常我们不会直接使用`Graph`来构建自己的图结构，而是会使用`GraphStageWithMaterializedValue`（或它的某个子类，接下来统称它们为 **GraphStage** ）。 **GraphStage** 是一个可重复使用的流处理操作图（a reusable graph stream processing operator），常用的 **GraphStage** 有两个：

- `GraphStageWithMaterializedValue`：有物化值的操作图，这样的图构造的 **Source** 签名类似：`Source[Out, Mat]`。
- `GraphStage`：不需要物化值的操作图，这样的图构造出的 **Source** 签名类似：`Source[Out, NotUsed]`；

```scala
abstract class GraphStageWithMaterializedValue[+S <: Shape, +M]
    extends Graph[S, M] {
  @throws(classOf[Exception])
  def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, M)

  protected def initialAttributes: Attributes = Attributes.none
}

abstract class GraphStage[S <: Shape] 
    extends GraphStageWithMaterializedValue[S, NotUsed] {
  final override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, NotUsed) =
    (createLogic(inheritedAttributes), NotUsed)

  @throws(classOf[Exception])
  def createLogic(inheritedAttributes: Attributes): GraphStageLogic
}
```

`GraphStageWithMaterializedValue`有一个抽像方法待实现，它返回图处理逻辑和物化值的元组，类型为：`(GraphStageLogic, M)`。`GraphStage`继承了`GraphStageWithMaterializedValue`，它实现了`createLogicAndMaterializedValue`方法并将物化值固定为`NotUsed`，同时提供`createLogic`供实现类创建图处理逻辑。

回到`GraphStageLogic`的`push`函数，通过它将数据元素发送到指定的输出端口。通常我们可以在`onPull`事件响应函数里调用它，`onPull`函数将由下游通过`pull`函数触发。

```scala
final protected def push[T](out: Outlet[T], elem: T): Unit
```

向指定的输出端口发射数据元素，在`pull`事件到达之前调用此方法两次将失败，在任何时间只能有一个未完成的推送请求。方法`isAvailable`可用于检查输出端口是否已准备好被推送。

## FileSource 实例讲解

一般不会通过`new`的方式直接创建一个`Source`出来，而是通过调用`Source.fromGraph`从一个预定义好的图创建，如：

```scala
def fromPath(
    f: Path, 
    chunkSize: Int, 
    startPosition: Long): Source[ByteString, Future[IOResult]] =
  Source
    .fromGraph(new FileSource(f, chunkSize, startPosition))
    .withAttributes(DefaultAttributes.fileSource)
```

这里通过Akka Streams自带的`FileSource`讲述Source图的定义过程。`FileSource`通过自定文件创建一个异步的文件读取源（Source）。

```scala
final case class IOResult(count: Long)

private[akka] final class FileSource(
      path: Path,
      chunkSize: Int,
      startPosition: Long)
    extends GraphStageWithMaterializedValue[SourceShape[ByteString], Future[IOResult]] {
  require(chunkSize > 0, "chunkSize must be greater than 0")
  val out = Outlet[ByteString]("FileSource.out")

  override val shape = SourceShape(out)
  // ....
}
```

`FileSource`要保存从文件实际读取字符数，所有它通过继承`GraphStageWithMaterializedValue`将计数通过物化值向下游传递。同时这个计数值不能阻塞整个流处理过程，所以物化值类型为：`Future[IOResult]`。

`Source[Out, Mat]`的类型签名只有输出类型，也许你会奇怪它实际要处理的数据源来自哪里？看到这里`FileSource(path: Path, ....)`的构造函数签名即可明白，它实际要处理的数据源就是`path`指定的文件路径，通常在实现自己的`Source`时，我们都要继承`Graph`的某个抽像子类，再在主构造函数里传入它要处理的实际数据源。

@@@note
`IOResult`的`count`变量是文件读取的位置（字节），实际读取文件字节数需要通过`count - startPosition`来获得，因为有可能并不是从文件头开始读取。
@@@

### createLogicAndMaterializedValue

```scala
  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[IOResult]) = {
    val ioResultPromise = Promise[IOResult]()
    val logic: GraphStageLogic = new GraphStageLogic(shape) with OutHandler {
      val buffer = ByteBuffer.allocate(chunkSize)
      val maxReadAhead = inheritedAttributes.get[InputBuffer](InputBuffer(16, 16)).max
      var channel: FileChannel = _
      var position = startPosition
      var eofEncountered = false
      var availableChunks: Vector[ByteString] = Vector.empty[ByteString]

      setHandler(out, this)
      // ....
    }
    (logic, ioResultPromise.future)
  }
```

变量`logic`和`ioResultPromise`，将作为`createLogicAndMaterializedValue`函数的返回值。`login`匿名实现了从`path`文件读取数据并发送到下游的功能逻辑，在图成功或失败完成时将`position`（读取到文件的位置偏移处（字节））

### GraphStageLogic 详解

**图属性变量**

```scala
val buffer = ByteBuffer.allocate(chunkSize)
val maxReadAhead = inheritedAttributes.get[InputBuffer](InputBuffer(16, 16)).max
var channel: FileChannel = _
var position = startPosition
var eofEncountered = false
var availableChunks: Vector[ByteString] = Vector.empty[ByteString]
```

- `buffer`：每次从文件里读取数据块缓存，读取的数据块将追加到`availableChunks`
- `maxReadAhead`：`availableChunks`长度，限制最多可向前读取的最大次数
- `channel`：低层`FileChannel`
- `position`：当前文件读取打针位置
- `eofEncoutered`：是否读到文件尾
- `availableChunks`：缓存的未处理数据块

**图的初始化**

```scala
  override def preStart(): Unit = {
    try {
      // this is a bit weird but required to keep existing semantics
      if (!Files.exists(path)) throw new NoSuchFileException(path.toString)

      require(!Files.isDirectory(path), s"Path '$path' is a directory")
      require(Files.isReadable(path), s"Missing read permission for '$path'")

      channel = FileChannel.open(path, StandardOpenOption.READ)
      channel.position(position)
    } catch {
      case ex: Exception =>
        ioResultPromise.trySuccess(IOResult(position, Failure(ex)))
        throw ex
    }
  }
```

校验`path`指定的文件是否存在、是否可读，并以指定的偏移量位置打开 `FileChannel`。

**onPull**

```scala
  override def onPull(): Unit = {
    if (availableChunks.size < maxReadAhead && !eofEncountered)
      availableChunks = readAhead(maxReadAhead, availableChunks)
    //if already read something and try
    if (availableChunks.nonEmpty) {
      emitMultiple(out, availableChunks.iterator, () => if (eofEncountered) success() else setHandler(out, handler))
      availableChunks = Vector.empty[ByteString]
    } else if (eofEncountered) success()
  }
```

1. 当收到下游拉取数据请求时，通过`readAhead`函数从`channel`中读取尽量多的数据块
2. 当`availableChunks`不为空，通过`emitMultiple`函数高效的将多个可用数据块 push 到下游。

```scala
final protected def emitMultiple[T](out: Outlet[T], elems: Iterator[T], andThen: () => Unit): Unit
```

`emitMultiple`函数可以简化需要发射多个元素的工作，它将在多个元素发射完成后恢复原有的处理程序（`OutHandler`），这样就不需要自己手动管理多个元素的发射状态。

**readAhead，读取数据** 

```scala
  private def success(): Unit = {
    completeStage()
    ioResultPromise.trySuccess(IOResult(position, Success(Done)))
  }
  /** BLOCKING I/O READ */
  @tailrec def readAhead(maxChunks: Int, chunks: Vector[ByteString]): Vector[ByteString] =
    if (chunks.size < maxChunks && !eofEncountered) {
      val readBytes = try channel.read(buffer, position)
      catch {
        case NonFatal(ex) =>
          failStage(ex)
          ioResultPromise.trySuccess(IOResult(position, Failure(ex)))
          throw ex
      }

      if (readBytes > 0) {
        buffer.flip()
        position += readBytes
        val newChunks = chunks :+ ByteString.fromByteBuffer(buffer)
        buffer.clear()

        if (readBytes < chunkSize) {
          eofEncountered = true
          newChunks
        } else readAhead(maxChunks, newChunks)
      } else {
        eofEncountered = true
        chunks
      }
    } else chunks
```

`readAhead`是一个尾递归函数，编译器在编译时会将其优化成循环调用，这样可避免栈溢出和性能问题。`readAhead`首先判断`chunks`是否未满或还有文件可读，若是则进行文件数据读取，否直接返回`chunks`。当`channel.read`返回的实际读出字节数`readBytes`大于0且不小于`chunkSize`，代表文件还有数据可继续读取，这时递归`readAhead`函数；否则设置 `eofEncoutered`为`true`并返回`newChunks`。

**onDownstreamFinish**

```scala
  override def onDownstreamFinish(cause: Throwable): Unit = {
    cause match {
      case _: SubscriptionWithCancelException.NonFailureCancellation =>
        success()
      case ex =>
        ioResultPromise.tryFailure(
          new IOOperationIncompleteException("Downstream failed before reaching file end", position, ex))
        completeStage()
    }
  }
```

当下游取消（cancel）时，会触发`onDownstreamFinish`函数，并通过`cause`参数告知下游取消时的异常，在此也完成当前`Source`。

**postStop**

```scala
  override def postStop(): Unit = {
    ioResultPromise.trySuccess(IOResult(position, Success(Done)))
    if ((channel ne null) && channel.isOpen) channel.close()
  }
```

在图停止后做清理工作，关闭打开的文件。

## 小结

完整`FileSource`源码见：[https://github.com/akka/akka/blob/master/akka-stream/src/main/scala/akka/stream/impl/io/IOSources.scala](https://github.com/akka/akka/blob/master/akka-stream/src/main/scala/akka/stream/impl/io/IOSources.scala)。
