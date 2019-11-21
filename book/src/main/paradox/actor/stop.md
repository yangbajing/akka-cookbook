# 怎样优雅的停止actor

## 返回 Behaviors.stopped

每处理一个消息，actor都将返回一个行为，可以通过返回 `Behaviors.stopped` 行为来告诉Actor系统此actor应被自动停止。

@@snip [stopped](../../../../../cookbook-actor/src/main/scala/cookbook/actor/stopped/StoppedMain.scala) { #stopped }

`Behaviors.stopped`被执行时将触发 `PostStop` 信号。

## Graceful Stop

通常，`PostStop`信号被作为非正常停止看待，若想在正常停止（Graceful Stop）时触发一些清理操作，可将清理函数（`cleanup`）传给`Behaviors.stopped`的重载版本。

@@snip [stopped](../../../../../cookbook-actor/src/main/scala/cookbook/actor/stopped/StoppedMain.scala) { #stopped-cleanup }

当 `PostStop` 信号被处理后，`cleanup`清理函数将紧接着执行。

@@snip [stopped](../../../../../cookbook-actor/src/main/scala/cookbook/actor/stopped/StoppedMain.scala) { #cleanup }

@@@note
Akka Typed不再提供`PoisonPill`消息来停止actor，推荐使用自定义消息并反回`Behaviors.stopped`行为来停止actor。
@@@
