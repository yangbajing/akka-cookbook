# Test

Akka 允许以异步或同步两种方式对 actor 进行测试，可以以异步方式在真实的 `ActorSystem` 上完成测试，也可以使用 `BehaviorTestkit` 在测试线程上同步完成。虽然对 Behavior 的行为逻辑进行同步测试更加直观具方便，但在多个 actor 之间或进行性能测试时，应首选在真实的 `ActorSystem` 上使用异步测试。

@@toc { depth=2 }

@@@ index

* [test-actor](test-actor.md)
* [custom-test-actor-system](custom-test-actor-system.md)
* [synchronous](synchronous.md)

@@@
