# 异常处理与监管（Supervise）

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

## 监管树（Supervise）

Akka Typed的监管策略里没有了经典（Untyped）actor的 **Escalate** 策略。就是说Akka Typed默认是不支持异常冒泡的，需要**watch**子actor，并监听`ChildFailed`信号并再手动重新抛出异常，或者不处理子actor的终止信息而自动抛出`DeathPactException`异常。

### 怎样向上冒泡异常

对于经典（Untyped）actor的 **Escalate** 监管策略，Akka Typed并未提供直接的支持，但有两种方式可以实现类似效果。

1. 不处理子actor的终止异常（`Terminated`或`ChildFailed`信号），这样actor将自动抛出`akka.actor.typed.DeathPactException`异常。但这样会使导致失败的原始异常被吞掉，因为这个异常将告知直接父actor（这某种程度上说是一件好事，这样就不会泄露实现细节）。
2. 监听子actor的终止异常，再重新抛出，这里父actor可以选择将导致子actor失败的原始直接抛出或做个封装。

### 示例代码

@@snip [WatchActor](../../../../../cookbook-actor/src/main/scala/cookbook/actor/fault/WatchActorMain.scala) { #WatchActorMain }

运行代码可以看到如下输出（输出内容已作简化）：

```
[20:25:03,475] [INFO] [cookbook.actor.fault.Child$] 
    [akka://watch/user/parent/child2] - started.
[20:25:03,475] [INFO] [cookbook.actor.fault.Child$]
    [akka://watch/user/parent/child1] - started.
....
[20:25:03,509] [INFO] [cookbook.actor.fault.Child$]
    [akka://watch/user/parent/child2] - stopped.
[20:25:03,513] [WARN] [akka.actor.SupervisorStrategy]
    [akka://watch/user/parent] - Received child actor 
    akka://watch/user/parent/child2 failed signal, original 
    exception is java.lang.RuntimeException: This is normal exception.
....
[20:25:04,490] [INFO] [cookbook.actor.fault.Child$]
    [akka://watch/user/parent/child1] - stopped.
....
[20:25:04,491] [INFO] [akka.actor.SupervisorStrategy]
    [akka://watch/user] - Received child actor 
    akka://watch/user/parent failed signal, original 
    exception is cookbook.actor.fault.ActorException: 
    cookbook.actor.fault.EscalateException: This is escalate exception.
```
