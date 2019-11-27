# 构建工具（sbt）

## 项目配置

编辑`project/plugins.sbt`，添加如下sbt插件：
```sbt
addSbtPlugin("com.lightbend.sbt" % "sbt-javaagent" % "0.1.5")
addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "0.7.2")
```

在`build.sbt`文件配置项目启用Akka gRPC和Java Agent：
```sbt
project
  .in(file("grpc"))
  .enablePlugins(AkkaGrpcPlugin, JavaAgent)
  .settings(
    javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.9" % "runtime;test",
    libraryDependencies ++= Seq(
        "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"))
```

要使工程支持 Akka gRPC，需要在sbt项目里启用`AkkaGrpcPlugin`插件，若需要在sbt里测试gRPC服务，还需要同时启用`JavaAgent`插件。

`jetty-alpn-agent`提供Akka HTTP 2需要的 **ALPN** 支持，使用 `javaAgents` 配置项使它在`runtime`和`test`两个执行范围可用。

当你需要在代码中引用`google.proto`或`scalapb.proto`定义的消息Protobuf类型时，需要引入`scalapb-runtime`库依赖。

## 目录结构

在一个 sbt 目录结构里，通过定义`.proto`定义的Protobuf消息和gRPC服务需要放在`protobuf`（或`proto`）目录，如下面目录结构：

```
├── src
│   ├── main
│   │   ├── protobuf
│   │   ├── resources
│   │   └── scala
│   └── test
│       ├── resources
│       └── scala
```

通过`.proto`定义的消息类型和gRPC服务，会在sbt的托管源码路径下生成相应的消息`case class`、服务接口和客户端实现：

```
└── target
    ├── scala-2.13
    │   ├── src_managed
    │   │   └── main
    │   │       ├── greeter
    │   │       │   ├── GreeterProto.scala
    │   │       │   ├── GreeterServiceClient.scala
    │   │       │   ├── GreeterServiceHandler.scala
    │   │       │   ├── GreeterService.scala
    │   │       │   ├── HelloReply.scala
    │   │       │   └── HelloRequest.scala
```

## 配置选项

### 可用配置是否生成gRPC服务端或客户端存根代码：

```sbt
// 同时生成客户端和服务端（默认）
akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client, AkkaGrpc.Server)

// 只生成客户端
akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client)

// 只生成服务端
akkaGrpcGeneratedSources := Seq(AkkaGrpc.Server)
```

### 可配置生成存根代码的语言（支持`Scala`或`Java`）：

```sbt
// 只生成Scala（默认）
akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Scala)

// 只生成Java
akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Java)

// 同时生成Scala和Java存根，需要启用 'flat_package' 特性。
// 这样生成的Scala和Java存根代码会在不同的包路径下，以避免命名冲突。
akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Scala, AkkaGrpc.Java)
akkaGrpcCodeGeneratorSettings := akkaGrpcCodeGeneratorSettings.value.filterNot(_ == "flat_package")
```

*这样做有一个优势，在你准备将gRPC做为公开SDK的底层传输协议而不是API接口时，可以选择使用Java来生成客户端代码，相对Scala来说，Java语言更通用，更适合用于SDK。*

### 传递参数给 ScalaPB

Akka-gRPC的sbt插件是基于ScalaPB的，Akka-gRPC已经设置了一些默认项目，但可以对它进行更改。可以通过`akkaGrpcCodeGeneratorSettings`配置项自定义它。比如：
```sbt
akkaGrpcCodeGeneratorSettings := akkaGrpcCodeGeneratorSettings.value.filterNot(_ == "flat_package") // 不启用 flat_package
akkaGrpcCodeGeneratorSettings += "single_line_to_proto_string" // 启用 single_line_to_proto_string
```

可以过滤掉某些`.proto`文件以不对它进行编译：
```sbt
  .settings(
    inConfig(Compile)(Seq(
      excludeFilter in PB.generate := "descriptor.proto"
    ))
  )
```

添加额外的Protobuf源码目录（默认将从`src/main/protobuf`和`src/main/proto`目录寻找`.proto`定义描述符文件。
```sbt
inConfig(Compile)(Seq(
  PB.protoSources += sourceDirectory.value / "proto_custom"  // src/main/proto_custom
))
```

### 从依赖加载`.proto`文件

为了避免在项目之间复制`.proto`文件，可以将它打包到库里面。然后在sbt的库依赖配置中使用`protobuf`配置范围，这样在`sbt-protoc`编译时就可以找到相应的`.proto`文件。
```scala
"com.thesamet.scalapb" %% "scalapb-runtime" % "0.9.1" % "protobuf"
```
