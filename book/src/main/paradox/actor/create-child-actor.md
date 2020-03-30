# 创建子 actor

创建一个子 actor 需要在 Behavior 中通过 `ActorContext[T]` 上的 `spawn` 方法来创建，而 `ActorContext[T]` 可以通过 `Behaviors.setup` 来引入。 

`ActorContext[T]`提供了`spawn`和`spawnAnonymous`来创建一个当前actor的直接子actor。`spawn`函数需要3个参数：

- `behavior`：待创建为actor的行为
- `name`：创建的actor的名字
- `props`：创建属性参数，可以配置线程调度器等

```scala
def spawn[U](behavior: Behavior[U], name: String, props: Props = Props.empty): ActorRef[U]
```
