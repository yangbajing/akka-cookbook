# 请求-响应模式

请求-响应是很经典的一个模式，Ping-Pong就是一个典型的请求响应模式的应用。其完整代码如下：

@@snip [Ping-Pong](../../../../../../cookbook-actor/src/main/scala/cookbook/actor/introduction/HelloWorld.scala) { #ping }

## 适用范围

1. 订阅actor并希望收到被订阅actor响应的多个消息

## 问题

1. 响应消息也许不匹配请求actor的类型限制，（参阅：适配响应 获取解决方案）
2. 很难检测到请求是否送达或已被处理
3. 当请求actor发起多次请求时，不能保存请求上下文信息（可在消息内加上请求id或引入新的独立接收者可解决此问题）
