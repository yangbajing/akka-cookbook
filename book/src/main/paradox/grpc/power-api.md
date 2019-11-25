# PowerApi

Akka gRPC是基于Akka HTTP实现的，怎样才可以访问HTTP Header呢？我们基于HTTP Header可以实现一些增强功能，比如： **调用链跟踪**、 **认证** 、 **鉴权** 等。这非常的简单，在sbt配置里添加`akkaGrpcCodeGeneratorSettings += "server_power_apis"`即可，这样生成的Akka gRPC自动生成的代码会额外提供`XxxxPowerApi`结尾的服务接口。

```scala
trait GreeterServicePowerApi extends GreeterService {
  
  def sayHello(
    in: greeter.HelloRequest,
    metadata: Metadata): scala.concurrent.Future[greeter.HelloReply]
  
  def itKeepsTalking(
    in: akka.stream.scaladsl.Source[greeter.HelloRequest, akka.NotUsed],
    metadata: Metadata): scala.concurrent.Future[greeter.HelloReply]
  
  def itKeepsReplying(
    in: greeter.HelloRequest,
    metadata: Metadata): akka.stream.scaladsl.Source[greeter.HelloReply, akka.NotUsed]
  
  def streamHellos(
    in: akka.stream.scaladsl.Source[greeter.HelloRequest, akka.NotUsed],
    metadata: Metadata): akka.stream.scaladsl.Source[greeter.HelloReply, akka.NotUsed]
  
  override def sayHello(in: greeter.HelloRequest): scala.concurrent.Future[greeter.HelloReply] = throw new GrpcServiceException(Status.UNIMPLEMENTED)
  
  override def itKeepsTalking(in: akka.stream.scaladsl.Source[greeter.HelloRequest, akka.NotUsed]): scala.concurrent.Future[greeter.HelloReply] = throw new GrpcServiceException(Status.UNIMPLEMENTED)
  
  override def itKeepsReplying(in: greeter.HelloRequest): akka.stream.scaladsl.Source[greeter.HelloReply, akka.NotUsed] = throw new GrpcServiceException(Status.UNIMPLEMENTED)
  
  override def streamHellos(in: akka.stream.scaladsl.Source[greeter.HelloRequest, akka.NotUsed]): akka.stream.scaladsl.Source[greeter.HelloReply, akka.NotUsed] = throw new GrpcServiceException(Status.UNIMPLEMENTED)
}
```

可以看到生成了`GreeterServicePowerApi`接口，它继承了`GreeterService`，并且默认的4个服务都已经有了默认实现：`throw new GrpcServiceException(Status.UNIMPLEMENTED)`；取而代之的是4个新的重载函数，它们都多了一个`Metadata`参数。`Metadata`接口定义如下：

```scala
@DoNotInherit trait Metadata {
  def getText(key: String): Option[String]
  def getBinary(key: String): Option[ByteString]
  def asMap: Map[String, List[MetadataEntry]]
}

class MetadataImpl(headers: immutable.Seq[HttpHeader] = immutable.Seq.empty) extends Metadata {
  // ....
}
```

其实`Metadata`保存的就是HTTP Header，通过它的实现类`MetadataImpl`构造函数需要`HttpHeader`列表来初始化既可看出。它提供了`getText`、`getBinary`和`asMap`方法提供了gRPC服务元数据（HTTP Header）的访问接口。

通过Akka gRPC生成的服务句柄类（`GreeterServicePowerApiHandler`），可以清晰的知道Akka gRPC是怎么创建`Metadata`的。
```scala
def partial(
    implementation: GreeterServicePowerApi,
    prefix: String = GreeterService.name,
    eHandler: ActorSystem => PartialFunction[Throwable, io.grpc.Status] = GrpcExceptionHandler.defaultMapper
  )(implicit mat: Materializer, system: ActorSystem): PartialFunction[HttpRequest, scala.concurrent.Future[HttpResponse]] = {
  implicit val ec: ExecutionContext = mat.executionContext
  import GreeterService.Serializers._

  def handle(request: HttpRequest, method: String): scala.concurrent.Future[HttpResponse] = method match {
    case "SayHello" =>
      val responseCodec = Codecs.negotiate(request)
      val metadata = new MetadataImpl(request.headers)
      GrpcMarshalling.unmarshal(request)(HelloRequestSerializer, mat)
        .flatMap(implementation.sayHello(_, metadata))
        .map(e => GrpcMarshalling.marshal(e, eHandler)(HelloReplySerializer, mat, responseCodec, system))
    
    case "ItKeepsTalking" =>
     // ....
  }
  // ....
}
```

`case "SayHello" =>`模式匹配既是构造`Metadata`和执行`SayHello`gRPC服务的代码逻辑。`val metadata = new MetadataImpl(request.headers)`一行代码通过`request.headers`构造了`MetadataImpl`对象。
