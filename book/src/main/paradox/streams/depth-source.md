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

`Source`通过类构造器实现了`Graph`接口的`traversalBuilder`和`shape`两个参数，其中`shape`限制了必需为一个`SourceShape[Out]`类型。

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

## 构建 `Source`

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

### GraphStage

**GraphStage** 代表一个可重复使用的流处理操作图（a reusable graph stream processing operator），常用的 **GraphStage** 有两个：

- `GraphStage`：不需要物化值的操作图，这样的图构造出的 **Source** 签名类似：`Source[Out, NotUsed]`；
- `GraphStageWithMaterializedValue`：有物化值的操作图，这样的图构造的 **Source** 签名类似：`Source[Out, Mat]`。

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
      var chunkCallback: Try[Int] => Unit = _
      var eofEncountered = false
      var availableChunks: Vector[ByteString] = Vector.empty[ByteString]

      setHandler(out, this)
      // ....
    }
    (logic, ioResultPromise.future)
  }
```

变量`logic`和`ioResultPromise`，将作为`createLogicAndMaterializedValue`函数的返回值。`login`匿名实现了从`path`文件读取数据并发送到下游的功能逻辑，在图成功或失败完成时将`position`（读取到文件的位置偏移处（字节））

## 小结

完整`FileSource`源码见：[https://github.com/akka/akka/blob/master/akka-stream/src/main/scala/akka/stream/impl/io/IOSources.scala](https://github.com/akka/akka/blob/master/akka-stream/src/main/scala/akka/stream/impl/io/IOSources.scala)。
