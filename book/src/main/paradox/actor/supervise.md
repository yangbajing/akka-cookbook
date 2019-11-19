# 处理 actor 异常

Akka实现了 [Let it crash](https://doc.akka.io/docs/akka/current/typed/fault-tolerance.html#fault-tolerance) 模式，它假定失败是不可避免的。我们不应该花费过多的精力去设计一个永不失败的系统，而是假定失败在所难免，当失败发生时应快速的响应失败并以正确的状态重新启动。

## 监管策略

通过Akka的监控机制，我们可以在actor发生异常时对其拦截并进行处理。默认的监管策略有：

- `resume`：忽略失败，并继续处理下一条消息（如果有）
- `restart`：重启actor
- `stop`：停止actor， **这是typed actor的默认行为，而untyped actor默认是重启**

## 错误（Validation Error）与失败（Failure）

但我们应该在发生任何异常时都应用Akka的监管策略吗？答案是否定的。对于错误（验证错误）与失败，两者之间有显著区别：

- **Validation Error**：验证错误通常是业务逻辑的一部分，不应该抛出异常！而应该建模的actor协议（消息）；
- **Failure**：对于 **失败**，应用 **让它崩溃** 模式是有用的。以一个干净地、可预测的全新状态恢复运行比通过大量的逻辑判断和try catch语句而污染了代码更有效。

## 示例

@@snip [FaultTolerance.scala](../../../../../cookbook-actor/src/main/scala/cookbook/actor/fault/FaultTolerance.scala) { #FaultTolerance }

通过`Behaviors.supervise`来包裹 behavior 来实现监管策略。多个监管策略可以使用`Behaviors.supervise`嵌套来实现。

```scala
Behaviors
  .supervise(Behaviors.supervise(FaultTolerance()).onFailure[RestartException](SupervisorStrategy.restart))
  .onFailure[StopException](SupervisorStrategy.stop)
```

运行示例输出内容如下（隐藏了部分日志输出）：

```shell
Actor[akka://fault-tolerance/user#0] started.
Actor[akka://fault-tolerance/user#0] Received Message.
Actor[akka://fault-tolerance/user#0] Received signal PreRestart
[2019-11-19 19:51:48,105] [ERROR] [akka.actor.typed.Behavior$] [fault-tolerance-akka.actor.default-dispatcher-3] [akka://fault-tolerance/user] - Supervisor RestartSupervisor saw failure: 可重启
....
Actor[akka://fault-tolerance/user#0] started.
[2019-11-19 19:51:49,070] [ERROR] [akka.actor.LocalActorRefProvider(akka://fault-tolerance)] [fault-tolerance-akka.actor.default-dispatcher-3] [akka.actor.LocalActorRefProvider(akka://fault-tolerance)] - guardian failed, shutting down system
....
[2019-11-19 19:51:49,071] [ERROR] [akka.actor.OneForOneStrategy] [fault-tolerance-akka.actor.default-dispatcher-3] [akka://fault-tolerance/user] - 退出
....
Actor[akka://fault-tolerance/user#0] Received signal PostStop
```