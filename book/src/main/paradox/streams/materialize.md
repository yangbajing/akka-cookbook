# 物化与物化值

## Materializer（物化器）

物化器负责将流蓝图（`RunnableGraph`）转换成可运行的流（通过在内部创建actor来异步执行）。通常来说首选系统范围的物化器，不需要我们手动创建它。

有两种方式来选择系统范围的物化器：

1. 通过隐式转换函数从ActorSystem获取，`akka.actor.typed.ActorSystem`和`akka.actor.ActorSystem`都支持。
    ```scala
    implicit val system: ActorSystem[Nothing]
    ```
2. 通过`SystemMaterializer`Akka扩展。
    ```scala
    // untyped ActorSystem
    SystemMaterializer(system).materializer
    // typed ActorSystem
    import akka.actor.typed.scaladsl.adapter._
    SystemMaterializer(system.toClassic).materializer 
    ```

## Materialized value（物化值）

当一个可运行图（`RunnableGraph`）被执行（`run`），它将返回一个值（结果）。而这个值被称为 **Materialized Value** （物化值）。物化值可以是`Sink`处理后的结果，也可以是`Source`内部记录的统计数据……。

```scala
Source[OUT, Mat]
Flow[IN, OUT, Mat]
Sink[IN, Mat]
```

这里的`OUT`、`IN`是在流处理过程中流通的每个数据元素的类型，整个流处结束后最终返回的结果是从`Source`、`Flow`、`Sink`各部分的`Mat`参数类型里选择的。`Source`、`Flow`、`Sink`每个的最后一个类型参数`Mat`就是物化值。带`Mat`后缀的操作函数提供了`combine`函数参数来选择要保留的物化值，如：`viaMat`、`toMat`。

`Source`的物化值一般用于记录数据源的内部状态；`Flow`的物化值通常都会将上游的物化值向下传递（`Keep.left`），也可以调用其它`Keep`函数里自定义向下传递的物化值；`Sink`的物化值通常用于汇总上游发送的数据。

```scala
def viaMat[T, Mat2, Mat3](flow: Graph[FlowShape[Out, T], Mat2])(
      combine: (Mat, Mat2) => Mat3): Source[T, Mat3]
def toMat[Mat2, Mat3](sink: Graph[SinkShape[Out], Mat2])(
      combine: (Mat, Mat2) => Mat3): RunnableGraph[Mat3]
```

## Keep

object `Keep`定义了4个便捷函数来选择保留图执行过程的哪个物化值，通常我们通过选择其中一个来作为调用`viaMat`或`toMat`函数时传递给`combine`参数的值。

```scala
object Keep {
  private val _left = (l: Any, _: Any) => l
  private val _right = (_: Any, r: Any) => r
  private val _both = (l: Any, r: Any) => (l, r)
  private val _none = (_: Any, _: Any) => NotUsed

  def left[L, R]: (L, R) => L = _left.asInstanceOf[(L, R) => L]
  def right[L, R]: (L, R) => R = _right.asInstanceOf[(L, R) => R]
  def both[L, R]: (L, R) => (L, R) = _both.asInstanceOf[(L, R) => (L, R)]
  def none[L, R]: (L, R) => NotUsed = _none.asInstanceOf[(L, R) => NotUsed]
}
```

下面代码，`source`的物化值保留了已读取文件的字符数，`sink`的物化值保存了实际入写文件的字节数。

@@snip [Materialize](../../../../../cookbook-streams/src/test/scala/cookbook/streams/MaterializeTest.scala) { #keep }

1. `Source`上的`via`和所有转换操作（不带`Mat`的）默认都会保留 **左边** 的物化值，这样`source`的物化值（记录从文件已读取字节数）就作为`graph`的物化值被传递；
2. `graph`被调用方作为 **左边** ，参数`sink`作为 **右边** ，`Keep.left`保留左边的物化值：记录从文件已读取字节数；
3. `graph`被调用方作为 **左边** ，参数`sink`作为 **右边** ，`Keep.right`保留右边的物化值：记录已写入文件字节数；
4. 通过`Keep.both`同时保留两边的物化值，作为一个 **tuple** 被返回；
5. 不保留任何物化值。只在不需要关心流的任务结束和状态时使用。
