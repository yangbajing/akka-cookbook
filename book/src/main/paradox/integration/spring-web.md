# 在 Spring 里使用 Akka Streams

Akka Streams 作为 Reactive Streams 的一种实现，可以很方便的与其它 Reactive Streams 实现进行互操作。而从 Spring 5 开始，也提供了 Reactive Streams 实现的版本： **WebFlow** 。 [**Alpakka Spring Web**](https://doc.akka.io/docs/alpakka/2.0/spring-web.html) 项目提供了对Spring Boot的支持，可以让我们在 Spring 项目中使用 Akka Streams。

## 添加 Akka Streams 支持

需要给Spring项目添加 Akka Streams 依赖：

@@dependency [sbt,Maven,Gradle] { group=com.lightbend.akka artifact=akka-stream-alpakka-spring-web_$scala.binary.version$ version=$alpakka.version$ }

在自定义配置：`SampleConfiguration`中实例化`ActorSystem`和`Materializer`两个 **Bean** 。

@@snip [SampleConfiguration](../../../../../integration/cookbook-spring/src/main/java/cookbook/integration/springweb/SampleConfiguration.java) { #SampleConfiguration }

## 编写控制器

在添加了 Akka Streams 支持后，我们就可以在控制器代码里直接返回 `Source[T, Mat]` 类型的结果了。

@@snip [SampleController](../../../../../integration/cookbook-spring/src/main/java/cookbook/integration/springweb/SampleController.java) { #SampleController }

## 运行程序

`SampleApplication.java` 代码如下，如通常的Spring Boot程序并无起二致。

@@snip [SampleApplication](../../../../../integration/cookbook-spring/src/main/java/cookbook/integration/springweb/SampleApplication.java) { #SampleApplication }

运行 `SampleApplication` 启动Spring Web程序，通过`curl`访问服务示例如下：

```
$ curl -i http://localhost:8080
HTTP/1.1 200 
Content-Type: text/plain
Transfer-Encoding: chunked
Date: Wed, 20 Nov 2019 03:47:44 GMT

Hello world!
Hello world!
Hello world!
Hello world!
Hello world!
```

可以看到响应头里有：`Transfer-Encoding: chunked`，这个结果是以流的形式将数据一块一块返回的。
