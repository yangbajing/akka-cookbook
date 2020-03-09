# 使用 Jackson 作为序列化

通过配置 `akka.actor.serialization-bindings` 指定特定的数据类型（及子类型）在远程通信中使用已配置的序列化工具。

@@snip [reference.conf](../../../../../cookbook-cluster/src/main/resources/reference.conf)

这里配置了两个 trait 分别使用 `jackson-cbor` 和 `jackson-json` 序列化方式，这两种序列化方式为 Akka 内置的序列化器。需要加入以下依赖使用：

@@dependency[sbt,Maven,Gradle] {
  group="com.typesafe.akka"
  artifact="akka-serialization-jackson_$scala.binary_version$"
  version="$akka.version$"
}  

## TODO
