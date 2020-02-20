# 怎样优雅的关闭 ActorSystem

使用 `CoordinatedShutdown` 可以优雅的方式关闭 `ActorSystem`。默认情况下，需要调用 `ActorSystem` 上的 `terminate` 方法才会触发 `CoordinatedShutdown`，但也可以设置为在 JVM 退出时自动运行（比如接收到操作系统的 **SIGTERM** 信号或Java虚拟机退出）。要使在程序退出时 `CoordinatedShutdown` 自动调用，需要如下配置：

## 启用 CoordinatedShutdown

确保启用如下配置，同时加载 `CoordinatedShutdown` 扩展。

```hocon
akka.jvm-shutdown-hooks = on
akka.coordinated-shutdown.run-by-jvm-shutdown-hook = on
akka.coordinated-shutdown.run-by-actor-system-terminate = on
akka.coordinated-shutdown.terminate-actor-system = on
```

有两种方式加载 `CoordinatedShutdown` 扩展：

1. 通过配置文件随 `ActorSystem` 自动加载：`akka.extensions += "akka.actor.CoordinatedShutdown"`
2. 在代码中手动调用 `CoordinatedShutdown(system)`

@@@note
当系统中应用了 `akka-cluster-typed`、`akka-cluster-sharding-typed` 等模块时，它们将在内部调用 `CoordinatedShutdown` 扩展。
@@@

## 使用 CoordinatedShutdown

### Phase（阶段）

`CoordinatedShutdown` 可通过 **Phase**(阶段) 来管理关闭时要执行任务的顺序。相同阶段内的多个任务将同时进行，不同阶段的任务以配置的顺序依次进行，默认阶段里 `before-service-unbind` 在关闭时最先执行。

默认，`CoordinatedShutdown` 定义了如下阶段：

- `before-service-unbind` 取消绑定自定义服务之前
- `service-unbind` 服务已取消绑定，这时不再接收新的请求，但已进入的请求将继续。如：HTTP 请求
- `service-requests-done` 已进入的请求全部执行完成
- `service-stop` 服务已停止
- `before-cluster-shutdown` 集群节点开始停止前
- `cluster-sharding-shutdown-region` 集群节点的分片区域开始关闭
- `cluster-leave` 集群节点发出 Leave 命令后
- `cluster-exiting` 集群节点开始退出
- `cluster-exiting-done` 集群节点退出结束
- `cluster-shutdown` 集群节点已关闭
- `before-actor-system-terminate` ActorSystem 终止前
- `actor-system-terminate` ActorSystem 已终止

@@@warning
注册到 `actor-system-terminate` 阶段的任务需要注意，这时候 `ActorSystem` 的线程调度和定时调度已经关闭。也就是说 `ActorSystem` 上的 `ExecutionContext` 和 `Scheduler` 已不可用。 
@@@

### 添加关闭任务

可以调用 `CoordinatedShutdown` 上的 `addTask` 方法来添加关闭任务，`addJvmShutdownHook` 方法来添加 JVM 退出时执行的关闭任务。

@@snip [ShutdownMain.scala](../../../../../cookbook-actor/src/main/scala/cookbook/actor/shutdown/ShutdownMain.scala) { #CoordinatedShutdown }

`addCancellableTask` 和 `addCancellableJvmShutdownHook` 方法可以添加可取消的关闭任务，它们将返回 `akka.actor.Cancellable` 。
