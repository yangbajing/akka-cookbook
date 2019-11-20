# 怎样向上冒泡异常

对于经典（Untyped）actor的 **Escalate** 监管策略，Akka Typed并未提供直接的支持，但有两种方式可以实现类似效果。

1. 不处理子actor的终止异常（`Terminated`或`ChildFailed`信号），这样actor将自动抛出`akka.actor.typed.DeathPactException`异常。但这样会使导致失败的原始异常被吞掉，因为这个异常将告知直接父actor（这某种程度上说是一件好事，这样就不会泄露实现细节）。
2. 监听子actor的终止异常，再重新抛出，这里父actor可以选择将导致子actor失败的原始直接抛出或做个封装。

## 示例代码

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
