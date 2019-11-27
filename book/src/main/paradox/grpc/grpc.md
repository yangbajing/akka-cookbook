# gRPC服务

## 定义消息和服务

@@snip [greeter.proto](../../../../../cookbook-grpc/src/main/protobuf/greeter/greeter.proto)

这里定义了两个消息：`HelloRequest`、`HelloReply`和`GreeterService`服务，`GreeterService`定义了4个服务方法，分别是：

- `SayHello`：经典的请求-响应服务，发送一个请求获得一个响应；
- `ItKeepsTalking`：持续不断的发送多个请求，在请求停止后获得一个响应；
- `ItKeepsReplying`：发送一个请求，获得持续不断的多个响应；
- `streamHellos`：持续不断的发送响应的同时也可获得持续不断的响应，可以通过`Source.queue`来获得可发送数据的`Queue`和获得响应数据的`Source`。

## 实现 gRPC 服务

@@snip [GreeterServiceImpl](../../../../../cookbook-grpc/src/main/scala/greeter/GreeterServiceImpl.scala) { #GreeterServiceImpl }

Akka gRPC提供了基于 Akka Streams 的API，更多 Akka Streams 的内容请参阅： @ref[Akka 流（Streams）](../streams/index.md)。

`itKeepsTalking`服务从客户端持续接收`HelloRequest`消息流，直到客户端主动停止（服务端也可以停止这个流，但这个服务语义上并未体现这一点）。这里收集客户端发送的所有元素并通过`Sink.seq`汇聚成一个序列，再构造`HelloReply`消息发送回客户端。

`itKeepsReplying`服务从客户端接收一个请求，持续不断的向客户端发送响应（一直到客户端主动终止）。这可以用来实现某些实时监控业务，当服务端收到对某个指标的监控请求后，服务端按一定的时间间隔持续不断的向客户端发送监控指标：

@@snip [GreeterClientTest](../../../../../cookbook-grpc/src/test/scala/greeter/GreeterClientTest.scala) { #sendMetrics }

`sendMetrics`服务模拟了一个监控指标发送，每隔1秒钟向客户端发送一个指标数据。

`streamHellos`服务从客户端获得持续的请求，同时可异步的向客户端返回持续的响应。我们可以基于它来实现心跳。

```scala
  def heartbeat(in: Source[Heartbeat, NotUsed]): Source[HeartbeatAck, NotUsed] = {
    val ref: ActorRef[Heartbeat] = getClientManager(in.clientId) // 
    in.map { req =>
      ref ! req
      HeartbeatAck()
    }
  }
```

`heartbeat`收到心跳请求后马上就像客户端返回一个`HeartbeatAck`的心跳确认请求，因为这里心跳只用于保持连接，返回一个空响应即可。而`ref ! req`将心跳请求发送给`ref`指代的一个客户端Manager业务处理actor，由actor实现心跳超时监控，可以通过配置actor的 **ReceiveTimeout** 来实现心跳超时判断。

## 测试 gRPC 服务

使用 Scalatest 对实现的4个gRPC服务进行测试，下面是单元测试代码：

@@snip [GreeterClientTest](../../../../../cookbook-grpc/src/test/scala/greeter/GreeterClientTest.scala) { #GreeterServiceClient }

在运行测试前需要先启动gRPC服务，在 Scalatest 的`beforeAll`函数内启动gRPC HTTP 2服务：

@@snip [GreeterClientTest](../../../../../cookbook-grpc/src/test/scala/greeter/GreeterClientTest.scala) { #GreeterService }

在构造 `GreeterServiceClient` gRCP客户端时需要提供`GrpcClientSettings`设置选项，这里通过调用`fromConfig`函数来从 **HOCON** 配置文件里读取gRPC服务选项，相应的`application-test.conf`配置文件内容如下：

@@snip [application-test.conf](../../../../../cookbook-grpc/src/test/resources/application-test.conf)

其中`use-tls`设置gRPC客户端不使用HTTPs建立连接，因为我们这个单元测试启动的gRPC HTTP服务不未启动SSL/TLS。
