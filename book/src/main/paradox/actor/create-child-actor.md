# 创建子 actor

`ActorContext[T]`提供了`spawn`和`spawnAnonymous`来创建一个当前actor的直接子actor。`spawn`函数需要3个参数：

- `behavior`：待创建为actor的行为
- `name`：创建的actor的名字
- `props`：创建属性参数，可以配置线程调度器等

```scala
def spawn[U](behavior: Behavior[U], name: String, props: Props = Props.empty): ActorRef[U]
```
