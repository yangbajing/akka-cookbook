# 访问文件

Akka Streams内置了 `FileIO` 工具库，可对文件以流的方式进行读、写。

`FileIO`提供了`toPath`和`fromPath`两个函数。`toPath`是一个Sink，它接收上游流过来的元素（`ByteString`）并将其写入（追加）文件；`fromPath`是一个Source，它将按下游的（要求）从文件读取数据。

@@snip [FileIOTest](../../../../../cookbook-streams/src/test/scala/cookbook/streams/file/FileIOTest.scala) { #file-io-top }

## FileIO.toPath 写数据到文件

这里构造了0到99（包含）个数字，并按一行一个的方式写入文件。

@@snip [FileIOTest](../../../../../cookbook-streams/src/test/scala/cookbook/streams/file/FileIOTest.scala) { #to-path }

这里使用了较复杂的方式来写入换行符`LINE_SEPARATER`，使用了`merge`来将两个源（Source）交叉合并的方式。其实也可以在`map(n => ByteString(n.toString))`将数字转换成字符串时直接把换行符给附加上去，就像这样：`map(n => ByteString(n.toString) ++ LINE_SEPARATOR)`。

@@@warning
需要注意`Source.repeat(....).take(....)`这里的`take`函数，在这个例子里是不可或缺的，若忘记限制`repeat`流的长度，则整个流将无限调用下去，直到写满你的磁盘。

*当然，在这里会被`futureValue`的超时异常而终止测试的执行，最终很有可能不会写满你的磁盘。*
@@@

`FileIO.toPath`在多个重载版本，在以未指定`options`参数时方式调用时，`options`的默认值为：`Set(WRITE, TRUNCATE_EXISTING, CREATE)`，它以写入的方式打开文件，同时若文件已存在则清空内容，不存在则创建。`toPath`完整版本函数签名如下，`startPosition`指定了写入指针的初始偏移量（字节）：

```scala
def toPath(f: Path, options: Set[OpenOption], startPosition: Long): Sink[ByteString, Future[IOResult]]
```

## FileIO.fromPath 从文件读数据

使用`Framing.delimiter`按指定的分隔标准从文件流读取数据（元素）。

@@snip [FileIOTest](../../../../../cookbook-streams/src/test/scala/cookbook/streams/file/FileIOTest.scala) { #from-path }

`FileIO.fromPath`也有重载版本，其完整版本函数签名如下：

```scala
def fromPath(f: Path, chunkSize: Int, startPosition: Long): Source[ByteString, Future[IOResult]]
```

- `chunkSize`：每次从文件里指定字节的块（缓冲）大小（字节）
- `startPosition`：指定读取指针偏移量（字节）

@@@note
使用`Framing.delimiter`从文件流里读取数据时需要注意一个问题，若文件不以你指定的分隔值结尾将会抛出异常：`Caused by: akka.stream.scaladsl.Framing$FramingException: Stream finished but there was a truncated final frame in the buffer`。当流读到文件末尾还未能找到指定的分隔值而不能结束分帧（framing）操作，而这时上游已经发送了完成（ **Finish** ）信号，而`Framing`还有未完成的buffer则会抛出此异常。

你将`Source.repeat(LINE_SEPARATOR)`的`take`函数参数指定为`99`再次运行测试代码就可重写这个问题。
@@@
