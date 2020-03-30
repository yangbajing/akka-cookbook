# 在 Actor 外部创建 actor

Akka Typed 已不允许通过 `ActorSystem` 的实例来创建 actor，推荐使用自定义的根 actor 来初始化整个 actor 树，并在构建 ActorSystem 时作为守卫 Behavior 传入：

```scala
val behavior: Behavior[T] = _ // 根actor，用于构建整个actor业务树
val system = ActorSystem(behavior, "typed")
```

但实际应用中，或许有充分的理由需要通过 ActorSystem 来创建actor，这有两种方式来实现：

- 使用 **SpawnProtocol**
- 为守卫actor提供自定义 **Spawn** 消息

## 使用 SpawnProtocol

使用 Akka 内置的 `SpawnProtocol` 作为 ActorSystem 的初始化 Behavior，就可以通过 `SpawnProtocol.Spawn` 消息来创建actor。

@@snip [SpawnProtocolMain](../../../../../cookbook-actor/src/main/scala/cookbook/actor/introduction/SpawnProtocolMain.scala) { #spawn-protocol }

## 为守卫 actor 提供自定义 **Spawn** 消息

使用 `SpawnProtocol` 虽然可以在ActorSystem外部创建actor，但却没法使用我们自己定义的守卫actor了。参照 `SpawnProtocol.Spawn` 为自己的守卫 actor 提供 `Spawn` 消息，这样就可以在 ActorSystem 外部创建 actor 了。

@@snip [CustomSpawnMain](../../../../../cookbook-actor/src/main/scala/cookbook/actor/introduction/CustomSpawnMain.scala) { #root-actor }

**通过此方式创建的所有actor都是守卫actor的子actor。**
