# StashBuffer 暂存待处理消息

在之前状态机的例子，在 **idle** 状态时通过`pendingMessages`属性来暂存待处理消息。这是一种很常见的业务状态，比如：等待数据库连接建立、等待后端服务启动完成等，都需要在这段时间内将请求的消息暂存下来，待服务可用时对其处理。Akka内置了对此的功能：`StashBuffer[T]`，它可以减少actor内你自己的定义可变状态属性，使代码更清晰、健壮……

@@snip [StashFSM](../../../../../cookbook-actor/src/main/scala/cookbook/actor/fsm/StashFSM.scala) { #StashFSM }  

当actor还未准备好服务时，通过`stash.stash(msg)`将消息暂存。待actor可以服务时，使用`stash.unstashAll(active())`来调用新的行为函数将所有暂存的消息按FIFO依次重放。

通过使用 **StashBuffer** ，不需要在actor内部自行使用一个集合变量来暂存消息，也不需要单独定义一个函数来处理这些暂存消息。只需要定义你本当需要的业务代码，待actor可用（可服务）时重放所有暂存消息即可。

@@snip [StashFSMTest](../../../../../cookbook-actor/src/test/scala/cookbook/actor/fsm/StashFSMTest.scala) { #StashFSMTest }

执行上面测试代码输出如下（已简化部分内容）：

```
[akka://StashFSMTest/user/fsm] - [idle] receive message: message 1
[akka://StashFSMTest/user/fsm] - [idle] receive message: message 2
[akka://StashFSMTest/user/fsm] - [active] receive message: message 1
[akka://StashFSMTest/user/fsm] - [active] receive message: message 2
[akka://StashFSMTest/user/fsm] - [active] receive message: message 3
[akka://StashFSMTest/user/fsm] - [active] receive message: message 4
[akka://StashFSMTest/user/fsm] - [idle] receive message: message 5
```
