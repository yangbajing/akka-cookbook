# 深入 Source

## Graph Source

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

`Source`还实现了`FlowOpsMat`特质，使得`Source`具有了一系列的`via`和`to`（及它们的变体）函数。

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
