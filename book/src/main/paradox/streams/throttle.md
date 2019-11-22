# 基于时间的处理

很多时候我们都想限制上游发送元素到下游的（生产）速度，Akka Streams内建了对此的支持。通过`Flow.throttle`的各个版本提供了多种策略的限流功能。

```scala
Source
  .fromIterator(() => Iterator.from(0))
  .throttle(5, 500.millis)
```

`throttle`限制每500毫秒内最多只生产5个元素（可发送5个到下游）。
