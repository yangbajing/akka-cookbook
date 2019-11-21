# 起步

Akka Streams是 [**Reactive Streams**](http://reactive-streams.org/) 的一种实现，提供了对反应式编程（见：[《反应式宣言》](https://www.reactivemanifesto.org/zh-CN)）的支持。

## 第一个示例

@@snip [Startup](../../../../../cookbook-streams/src/main/scala/cookbook/streams/startup/Examples.scala) { #simplify }

运行上面的示例，将在终端从0开始按递增数字每个数字并将其转换成字符串打印10行，每个字符串一行。

**明细版**

@@snip [Startup](../../../../../cookbook-streams/src/main/scala/cookbook/streams/startup/Startup.scala) { #Startup }

这个版本拆分了各个步骤：

1. `source: Source[Int, NotUsed]`：源，生产数据元素待下游消费
2. `flow: Flow[Int, String, NotUsed]`：流，将数字转换成字符串
3. `flowTake: Flow[String, String, NotUsed]`：流，限制只取头10个元素
4. `sink: Sink[String, Future[Done]]`：汇，生产的数据最终流到这里并被消费
5. `graph: RunnableGraph[Future[Done]]`：将`source`、`flow`、`flowTake`、`sink`各部分连接起来就形成了一个可运行的执行蓝图

Akka Streams是lazy的，每次运行都全新的，中间状态将不会把保存。当调用`graph`的`.run`函数运行时需要上下文内有一个隐式参数：`Materializer`，它将负责实际执行这个蓝图。`Materializer`将创建一个actor异步执行。

## Graph

Akka Streams 使用 **Graph** （图）来抽像流处理的逻辑。

```scala
trait Graph[+S <: Shape, +M] {

  type Shape = S @uncheckedVariance

  def shape: S

  private[stream] def traversalBuilder: TraversalBuilder

  def withAttributes(attr: Attributes): Graph[S, M]

  // ....
}
```

每个图都会有一个形状（Shape）组成，形状描述的图的入口（inlets、输入端口）和出口（outlets、输出端口）。

```scala
abstract class Shape {
  def inlets: immutable.Seq[Inlet[_]]
  def outlets: immutable.Seq[Outlet[_]]
  // ....
}
```

多个形状的输入与输出端口可相互连接，使多个图（通过形状）连接到一起形成了可重复使用的蓝图。当图里所有开关的端口都有连接，我们称这个图已闭合（`ClosedShape`），这样的图就可以成为一个可运行的图（`RunnableGraph`）。

#### RunnableGraph

`RunnableGraph`是可运行的图，调用它的`run`函数将实际执行

```scala
final case class RunnableGraph[+Mat](override val traversalBuilder: TraversalBuilder) extends Graph[ClosedShape, Mat] {}
```

可运行图必须是逻辑上已闭合的图，它不能有未连接的端口。
