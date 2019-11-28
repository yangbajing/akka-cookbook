# 监视 actor 的停止状态

一个actor可以通过`context.watch`函数监听其它actor的终止情况，在`Terminated`或`ChildFailed`信号发出时对其进行捕获并处理。

@@@note
Akka Typed默认不会 **watch** 创建的子actor，若需要监听子actor的终止信号需要手动 **watch**。
@@@

`Terminated`信号通过`ref`属性告知监听者是哪个actor已终止。`ChildFailed`信号作为`Termianted`的子类，它除了`ref`指出是哪个actor已终止外，还通过`cause`属性告知子actor终止时被抛出的异常。

示例代码请见： @ref[怎样向上冒泡异常#示例代码](supervise.md#示例代码)。
