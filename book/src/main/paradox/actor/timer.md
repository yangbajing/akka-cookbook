# TimerScheduler 发送消息

## 设置默认消息超时时间

通过调用`ActorContext[T]`的`setReceiveTimeout`函数可以设置actor（当前actor实例）的默认消息超时时间，并在超时时间被触发时向actor发送指定消息。

```scala
def setReceiveTimeout(timeout: FiniteDuration, msg: T): Unit
```
## actor定时发送消息给自己

Akka Typed提供了`TimerScheduler[T]`来启动计时器将指定消息发送给actor自己，支持单次、多次两种发送模式，而多次发送模式又支持两种计时策略：

1. 固定延迟（**fixed-delay**）：发送后续消息之章的延迟始终（不小于）为给定的值，使用`startTimerWithFixedDelay`函数
2. 固定速率（**fixed-rate**）：一段时间内执行的频率满足给定的间隔，使用`startTimerAtFixedRate`函数

如果不确定使用哪一个，建议选择`startTimerWithFixedDelay`。因为 **固定速率** 在长时间的垃圾收集暂停后可能会导致计划消息的突发，这在最坏的情况下可能会导致系统上出现预期外的负载。通常首选具有 **固定延迟** 的调度计划。

当使用固定延迟时，如果由于某种原因，调度延迟超过指定的时间，则它不会补偿消息之间的延迟。发送后续消息之间的延迟总是（至少）给定的延迟。从长远来看，消息的频率通常会略低于指定延迟的倒数。

固定延迟执行适用于需要“平滑度”的重复性活动。换句话说，它适用于短期内比长期内保持频率准确更为重要的活动。

使用固定速率时，如果先前的消息延迟太长，它将补偿后续任务的延迟。在这种情况下，实际的发送间隔将不同于传递给 **固定速率** 方法的间隔。

如果任务延迟超过间隔时间，则在前一个任务之后立即发送后续消息。这还会导致在长时间的垃圾收集暂停或JVM暂停时的其他原因之后，当进程再次唤醒时，将执行所有“错过”的任务。例如，间隔`1`秒的 **固定速率** 和暂停`30`秒的进程将导致连续快速发送30条消息以赶上之前错过的调度。从长远来看，执行频率正好是指定间隔的倒数。

固定速率执行适用于对绝对时间敏感或执行固定数量执行的总时间很重要的重复活动，例如每秒计时一次并持续10秒的倒计时计时器。

@@snip [Timer](../../../../../cookbook-actor/src/main/scala/cookbook/actor/timer/Timer.scala) { #Timer }

测试程序：

@@snip [TimerTest](../../../../../cookbook-actor/src/test/scala/cookbook/actor/timer/TimerTest.scala) { #TimerTest }

上面程序的测试输出如下（已简化部分内容）：

```
[akka://TimerTest/user/$a] - Receive message: TimerTrigger
[akka://TimerTest/user/$a] - Receive message: TimerTrigger
[akka://TimerTest/user/$a] - Receive message: SingleTrigger
[akka://TimerTest/user/$a] - Receive message: TimerTrigger
[akka://TimerTest/user/$a] - Receive message: TimerTrigger
[akka://TimerTest/user/$a] - Receive message: CancelAllTimer
[akka://TimerTest/user/$a] - Receive message: ReceiveTimeout
```
