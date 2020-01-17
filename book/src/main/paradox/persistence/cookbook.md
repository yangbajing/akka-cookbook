# Akka Persistence Cookbook

## How to implement pagination query in the Akka Persistence

在 Akka Persistence 中，数据都缓存在服务内存（状态），后端存储的都是一些持久化的事件日志，没法使用类似 SQL 一样的 DSL 来进行分页查询。利用 Akka Streams 和 Actor 我们可以通过编码的方式来实现分页查询的效果，而且这个分页查询还是分步式并行的……

### `EventSourcedBehavior`

Akka Persistence的`EventSourcedBehavior`里实现了**CQRS**模型，通过`commandHandler`与`eventHandler`解耦了命令处理与事件处理。`commandHandler`处理传入的命令并返回一个事件，并可选择将这个事件持久化；若事件需要持久化，则事件将被传给`eventHandler`处理，`eventHandler`处理完事件后将返回一个“新的”状态（也可以不更新，直接返回原状态）。

```scala
def apply[Command, Event, State](
      persistenceId: PersistenceId,
      emptyState: State,
      commandHandler: (State, Command) => Effect[Event, State],
      eventHandler: (State, Event) => State): EventSourcedBehavior[Command, Event, State]
```

### Build models

以我们习惯的数据库表建模来说，我们会有以下一张表：

```sql
create table t_config
(
    data_id     varchar(64),
    namespace   varchar(64) not null,
    config_type varchar(32) not null,
    content     text        not null,
    constraint t_config_pk primary key (namespace, data_id)
);
create index t_config_idx_data_id on t_config (data_id);
```

`ConfigManager` actor 可以看作 `t_config` 表，它的 `entityId` 就是 `namespace`， **State** 里保存了所有记录的主键值（`ConfigManagerState`），这就相当于 `t_config` 表的 `t_config_idx_data_id` 索引。

而 `ConfigEntity` actor 可看作 `t_config` 表里存储的记录，每个 actor 实例就是一行记录。它的 `entityId` 由 `namespace` + `data_id` 组成，这就相当于 `t_config` 表的 `t_config_pk` 复合主键。
这里我们定义两个 `EventSourcedBehavior`：

- `ConfigManager`：拥有所有配置ID列表，并作为 State 保存在 EventSourcedBehavior
- `ConfigEntity`: 拥有每个配置数据，并作为 State 保存在 EventSourcedBehavior

### Implementation

这里先贴出 `ConfigManager` 和 `ConfigEntity` 的部分代码，接下来再详解怎样实现分页查询。

**ConfigManager**

```scala
object ConfigManager {
  sealed trait Command extends CborSerializable
  sealed trait Event extends CborSerializable
  sealed trait Response extends CborSerializable

  final case class Query(dataId: Option[String], configType: Option[String], page: Int, size: Int) extends Command
  final case class ReplyCommand(in: AnyRef, replyTo: ActorRef[Response]) extends Command
  private final case class InternalResponse(replyTo: ActorRef[Response], response: Response) extends Command

  case class ConfigResponse(status: Int, message: String = "", data: Option[AnyRef] = None) extends Response

  final case class ConfigManagerState(dataIds: Vector[String] = Vector()) extends CborSerializable

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey("ConfigManager")
}

import ConfigManager._
class ConfigManager private (namespace: String, context: ActorContext[Command]) {
  private implicit val system = context.system
  private implicit val timeout: Timeout = 5.seconds
  import context.executionContext
  private val configEntity = ConfigEntity.init(context.system)

  def eventSourcedBehavior(): EventSourcedBehavior[Command, Event, ConfigManagerState] =
    EventSourcedBehavior(
      PersistenceId.of(TypeKey.name, namespace),
      ConfigManagerState(), {
        case (state, ReplyCommand(in, replyTo)) =>
          replyCommandHandler(state, replyTo, in)
        case (_, InternalResponse(replyTo, response)) =>
          Effect.reply(replyTo)(response)
      },
      eventHandler)
  
  private def processPageQuery(
      state: ConfigManagerState,
      replyTo: ActorRef[Response],
      in: Query): Effect[Event, ConfigManagerState] = {
    val offset = if (in.page > 0) (in.page - 1) * in.size else 0
    val responseF = if (offset < state.dataIds.size) {
      Source(state.dataIds)
        .filter(dataId => in.dataId.forall(v => v.contains(dataId)))
        .mapAsync(20) { dataId =>
          configEntity.ask[Option[ConfigState]](replyTo =>
            ShardingEnvelope(dataId, ConfigEntity.Query(in.configType, replyTo)))
        }
        .collect { case Some(value) => value }
        .drop(offset)
        .take(in.size)
        .runWith(Sink.seq)
        .map(items => ConfigResponse(IntStatus.OK, data = Some(items)))
    } else {
      Future.successful(ConfigResponse(IntStatus.NOT_FOUND, data = Some(Nil)))
    }
    context.pipeToSelf(responseF) {
      case Success(value) => InternalResponse(replyTo, value)
      case Failure(e)     => InternalResponse(replyTo, ConfigResponse(IntStatus.INTERNAL_ERROR, e.getLocalizedMessage))
    }
    Effect.none
  }
}
```

**ConfigEntity**

```scala
object ConfigEntity {
  case class ConfigState(namespace: String, dataId: String, configType: String, content: String)

  sealed trait Command extends CborSerializable
  sealed trait Event extends CborSerializable

  final case class Query(configType: Option[String], replyTo: ActorRef[Option[ConfigState]]) extends Command

  final case class ConfigEntityState(config: Option[ConfigState] = None) extends CborSerializable

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey("ConfigEntity")
}

import ConfigEntity._
class ConfigEntity private (namespace: String, dataId: String, context: ActorContext[Command]) {
  def eventSourcedBehavior(): EventSourcedBehavior[Command, Event, ConfigEntityState] =
    EventSourcedBehavior(PersistenceId.of(TypeKey.name, dataId), ConfigEntityState(), commandHandler, eventHandler)

  def commandHandler(state: ConfigEntityState, command: Command): Effect[Event, ConfigEntityState] = command match {
    case Query(configType, replyTo) =>
      state.config match {
        case None =>
          Effect.reply(replyTo)(None)
        case Some(config) =>
          val resp = if (configType.forall(v => config.configType.contains(v))) Some(config) else None
          Effect.reply(replyTo)(resp)
      }
  }
}
```
 
`ConfigManager#processPageQuery` 函数实现了大部分的分页查询逻辑（有部分逻辑需要由 `ConfigEntity` 处理）。

```scala
val offset = if (in.page > 0) (in.page - 1) * in.size else 0
val responseF = if (offset < state.dataIds.size) {
  // process paging
} else {
  Future.successful(ConfigResponse(IntStatus.OK, data = Some(Nil)))
}
```

这里首先获取实际的分页数据偏移量 `offset` ，再于 `ConfigManager` 状态里保存的 `dataIds` 的大小进行判断，若 `offset` < `state.dataIds.size` 则我们进行分页逻辑，否则直接返回一个空列表给前端。

```scala
  Source(state.dataIds)
    .filter(dataId => in.dataId.forall(v => v.contains(dataId)))
    .mapAsync(20) { dataId =>
      configEntity.ask[Option[ConfigState]](replyTo =>
        ShardingEnvelope(s"$namespace@$dataId", ConfigEntity.Query(in.configType, replyTo)))
    }
    .collect { case Some(value) => value }
    .drop(offset)
    .take(in.size)
    .runWith(Sink.seq)
    .map(items => ConfigResponse(IntStatus.OK, data = Some(items)))
```

这个 Akka Streams 流即是分页处理的主要实现，若是SQL的话，它类似：

```sql
select * from t_config where data_id like '%"in.dataId"%' offset "offset" limit "in.size"
```

`.mapAsync` 在流执行流程中起了20个并发的异步操作，将委托每个匹配的 `ConfigEntity` （由`s"$namespace@$dataId"`生成`entityId`）执行 `config_type` 字段的查询。这样，完整的SQL语句类似：

```sql
select * from t_config where data_id like '%"in.dataId"%' and config_type = "in.configType" offset "offset" limit "in.size"
``` 

`ConfigEntity` 对 `config_type` 部分的查询逻辑实现如下：

```scala
case Query(configType, replyTo) =>
  state.config match {
    case None =>
      Effect.reply(replyTo)(None)
    case Some(config) =>
      val resp = if (configType.forall(v => config.configType.contains(v))) Some(config) else None
      Effect.reply(replyTo)(resp)
  }
```

若`in.configType`为空，既不需要判断 `config_type` 这个字段，直接返回 `Some(config)` 即可，而这时的SQL语句类似：

```sql
select * from t_config where data_id like '%"in.dataId"%' and true offset "offset" limit "in.size"
```

_**Tip**这里有个小技巧，对于 `Option[T]` 字段的判断，直接使用了 `.forall` 方法，它等价于：_

```scala
option match {
  case Some(x) => p(x)
  case None    => true
}
```

