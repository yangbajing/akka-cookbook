# 实例：使用 Akka Streams 解 Top K 问题

## 问题描述

服务器上有一个 movies.csv 文件，里面保存了每部电影的评分（为了简化和专注问题，CSV文件每一行只有两个字段：movieId和rating）。文件通过HTTP服务器发布。要求从文件内找出排名最高的10部电影。

## 解法1：全量排序求Top 10

通过 `wget`、`curl` 等工具先将文件下载到本地，再读出文件内所有行并解析出 `movieId`和`rating` 字段，按 `rating` 字段排序并求得分最高的 10 部电影。这种方法逻辑很简单，实现代码如下：

```scala
final case class Movie(id: String, rating: Double)

val top10 = scala.io.Source.fromFile("/tmp/movies.csv").getLines()
  //.drop(1) // if csv header exists.
  .flatMap { line => 
    line.split(',') match {
      case Array(movieId, rating) => Try(Movie(movieId, rating.toDouble)).toOption  
      case _ => None
    }
  }
  .toVector
  .sortWith(_.rating > _.rating)
  .take(10)
```

## 解法2：遍历一次文件求出Top 10

因为我们只是找出得分最高的10部电影，可以预先定义一个有序 `top10` 集合，在遍历 `movies.csv` 的每一部电影时将其与 `top10` 集合里得分最低的一部电影比较。若得分大于集合里最低的那部电影，则将集合里得分最低的电影去掉，将将当前电影加入集合。这样，我们只需要遍历一次文件即可获得得分最高的10部电影。

```scala
final case class Movie(id: String, rating: Double)

var top10 = Vector[Movie]()
scala.io.Source.fromFile("/tmp/movies.csv").getLines()
  //.drop(1) // if csv header exists.
  .flatMap { line => 
    line.split(',') match {
      case Array(movieId, rating) => Some(Movie(movieId, rating.toDouble))  
      case _ => None
    }
  }
  .foreach { movie =>
    top10 = if (top10.size < 10) (movie +: top10).sortWith(_.rating < _.rating)
      else if (top10.head.rating > movie.rating) top10
      else (movie +: top10.tail).sortWith(_.rating < _.rating)
  }
```

## 解法3：使用Akka Streams异步求出Top K个得分最高的电影

`FileIO` 是Akka Streams自带的一个文件读、写工具类，可以从一个文件生成 `Source[ByteString, Future[IOResult]]` 或将 `Source[ByteString, Future[IOResult]]` 写入文件。`Framing.delimiter` 可以从Akka Streams 的 `ByteString` 流以指定分隔符（`\n`）按行提取内容，并将每一行数据发送到流程的下一步骤。

@@@note
这里 `Framing.delimiter` 的第3个参数 `allowTruncation` 需要设置为 **true** ，否则文件在不以 `\n` 结尾的情况下将抛出以下异常：
```scala
akka.stream.scaladsl.Framing$FramingException: Stream finished but there was a truncated final frame in the buffer
``` 

如果设置为 **false**，则当正在解码的最后一个帧不包含有效的分隔符时，此流将失败，而不是返回截断的帧数据。
@@@

@@snip [TopKForFile.scala](../../../../../cookbook-streams/src/main/scala/cookbook/streams/topk/TopKForFile.scala) { #TopKForFile }

这里使用 alpakka-csv 来将 `ByteString` 数据流转换成 CSV 数据格式，可以在 [https://doc.akka.io/docs/alpakka/current/data-transformations/csv.html](https://doc.akka.io/docs/alpakka/current/data-transformations/csv.html) 找到这个库的详细使用说明。

@@snip [TopKUtils.scala](../../../../../cookbook-streams/src/main/scala/cookbook/streams/topk/TopKUtils.scala) { #toMovie }

### 自定义Sink：`TopKSink`

`.runWith(new TopKSink(10))` 调用自定义的 Akka Streams `Sink` 来获得得分最高的10部电影。让我们先来看看 `TopKSink` 的实现：

@@snip [TopKSink.scala](../../../../../cookbook-streams/src/main/scala/cookbook/streams/topk/TopKSink.scala) { #TopKSink }

通过继承 `GraphStageWithMaterializedValue` 抽像类，可以定义一个返回特定结果的自定义 `Sink`，否则流处理结果默认为 `NotUsed`。

```scala
  override def createLogicAndMaterializedValue(
      inheritedAttributes: Attributes): (GraphStageLogic, Future[List[Movie]])
```

函数 `createLogicAndMaterializedValue` 实现 `Sink` 处理逻辑并返回 `Sink` 阶段的处理逻辑 `GraphStageLogic` 和获得的 Top K 结果 `Future[List[Movie]]`，流执行后的结果（通过调用 `.runWith`）是一个异步结果（`Future`），这样将不会阻塞调用线程。

在 `TopKSink` 里，使用了一个优先队列来保存 TOP K 部电影，通过提供一个自定义的隐式值 `movieOrdering` 告知 `PriorityQueue` 怎样排序。得分低的电影排在队列头，这样在有一部新电影时我们只需要和队列头比较评分即可，若新电影评分更高我们就将它插入队列并将队列头删去，下面为电影入队操作具体代码：

@@snip [TopKSink.scala](../../../../../cookbook-streams/src/main/scala/cookbook/streams/topk/TopKSink.scala) { #append-movie }

## 解法4：通过 Akka HTTP 在下载文件的同时求出Top K个得分最高的电影

Akka HTTP提供了 HTTP Client/Server 实现，同时它也是基于 Akka Streams 实现的。上一步我们已经定义了 `TopKSink` 来消费流数据，而通过 Akka HTTP Client 获得的响应数据也是一个流（`Source[ByteString, Any]`）。我们可以将获取 `movies.csv` 文件的 HTTP 请求与取得分最高的K部电影两个任务结合到一起，**实现内存固定、处理数据无限的 Top K 程序（假设网络稳定不会断开）**。 

@@snip [TopKForAkkaHTTP.scala](../../../../../cookbook-streams/src/main/scala/cookbook/streams/topk/TopKForAkkaHTTP.scala) { #TopKForAkkaHTTP }

## 小结

本文使用4种方式来求解 Top K 问题，从简单粗暴的全量读入内存并排序；到不使用排序通过一次遍历获得 Top K；再使用 Akka Streams 以流式方式异步获得；最后，通过结合 Akka HTTP 和 Akka Streams，可以HTTP请求的同时计算 Top K。

有关 Akka HTTP 更多内容可阅读 [《Scala Web 开发——基于Akka HTTP》](https://www.yangbajing.me/scala-web-development/) 。
