# 通过 ActorSystem 创建 actor

Akka Typed已不允许通过 ActorSystem 的实例来创建actor，推荐自定义的根actor来初始化整个actor树，并在构建 ActorSystem 作为守卫行为传入：

```scala
val behavior: Behavior[T] = _ // 根actor，用于构建整个actor业务树
val system = ActorSystem(behavior, "typed")
```

但实际应用中，允许有充分的理由需要通过 ActorSystem 来创建actor，这有两种方式：

- 使用 **SpawnProtocol**
- 为守卫actor提供自定义 **Spawn** 消息

## 使用 SpawnProtocol

@@snip [SpawnProtocolMain](../../../../../cookbook-actor/src/main/scala/cookbook/actor/introduction/SpawnProtocolMain.scala) { #spawn-protocol }

将Akka内置的`SpawnProtocol`作为ActorSystem的初始化行为，就可以通过`SpawnProtocol.Spawn`消息来创建actor。

## 为守卫actor提供自定义 **Spawn** 消息

使用`SpawnProtocol`虽然可以在ActorSystem外部创建actor，但却没法使用我们自己定义的守卫actor了。参照 `SpawnProtocol.Spawn` 为自己的守卫actor提供`Spawn`消息，这样就可以在ActorSystem外部创建actor了。

@@snip [CustomSpawnMain](../../../../../cookbook-actor/src/main/scala/cookbook/actor/introduction/CustomSpawnMain.scala) { #root-actor }

**通过此方式创建的所以actor都为是守卫actor的子actor。**
