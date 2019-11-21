# 行为切换与状态机

Akka Typed使用`Behavior`替代了`Actor`，通过函数式的方式来构建消息处理行为，每次消息处理后都需要返回下一个（消息处理）行为。经典（Untyped）actor的`become`、`unbecome`和 **FSM** （状态机）都不再需要了，因为通过返回下一个行为时就已经可以实现以上功能。

下面定义了一个简单的状态机actor，它有两个状态行为：`idle`和`active`，分别由`passive`和`running`两个事件消息来触发切换。

@@snip [FiniteStateMachine](../../../../../cookbook-actor/src/main/scala/cookbook/actor/fsm/FiniteStateMachine.scala) { #FiniteStateMachine }  

这里使用了`FiniteStateMachine`类的方式来定义`Behavior`行为集，通过定义不同的类函数来实现在不同事件消息情况下的行为，这样还可通过类属性来保存actor的内部状态。初始时在`Behavior.setup`构造块里使用了`idle`这个行为函数，当收到`running`消息时通过返回`active()`函数实现了行为切换，就类似经典actor的`become`一样。

当你的actor有多个行为函数，或代码逻辑（行）比较多时，通过这种 **类** 的方式来管理它们能使行代码更加清晰，这是一种良好的实践！

@@@note
可以在`FiniteStateMachine`类构造块里作actor的初始化工作，也可以在`Behaviors.setup`里初始化后将数据通过构造参数传给`FiniteStateMachine`，就像`context`参数一样。

但需要注意的时，如何actor重启，`pendingMessages`将不会保存之前的状态（内容），因为`FiniteStateMachine`将被重新实例化。这可以通过某种持久化机制来解决，如： **Akka Persistence** ；或者在 `PreRestart` 信号处理函数里自行保存数据，再在类构造时读出。
@@@

测试代码如下：

@@snip [FiniteStateMachine](../../../../../cookbook-actor/src/test/scala/cookbook/actor/fsm/FiniteStateMachineTest.scala) { #FiniteStateMachine }  
