# Akka Persistence

Akka Persistence 支持对 Actor 内部状态进行持久化管理，这样在系统挂掉或重启时就不会丢失数据。而对于 At-Least-Once-Delivery，也需要有 Akka Persistence 的支持。

要使用 Akka Persistence，需要在项目里添加 `akka-persistence-typed` 依赖

@@dependency[sbt,Maven,Gradle] { group=com.typesafe.akka artifact=akka-persistence-typed_$scala.binary_version$ version=$akka.version$ }

@@toc { depth=3 }

@@@ index

* [event-source](event-sourcing.md)
* [cookbook](cookbook.md)

@@@
