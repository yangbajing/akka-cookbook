# Event Sourcing


Akka Persistence 使有状态的 actor 在重启后能保持其内部状态，如：JVM崩溃、被监控策略或手动停止及集群迁移。Akka Persistence 默认只存储事件日志（Event），状态通过事件日志生成。当系统运行很长时间后，事件日志会非常多，重启将回放所有日志，这可能会造成系统启动时间过长。Akka Persistence 还提供了快照将状态（State）存储下来，这样系统重启时将以最新的快照为基础，并只重放从快照生成时间点开始的事件日志，这样可以显著加快系统恢复的时间。而且，我们还可以选择删除快照时间点之前的所有事件日志来节省存储空间。

## `EventSourcedBehavior`
