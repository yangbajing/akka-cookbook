# Streams 分组

## grouped

通过`grouped`函数可将上游发送的元素按指定个数分组，这在很场景下可作为一个优化策略。比如：批量写入数据库。

@@snip [grouped](../../../../../cookbook-streams/src/test/scala/cookbook/streams/group/GroupTest.scala) { #grouped }

## groupedWithin

若上游长时间没有元素被发送，很有可能下游将被永久的挂在那里，形成一种假死的状态。这时可通过`groupedWithin`函数传递一个超时时间，它将在指定的分组数量或超时时间两者之一达到时形成一个分组并将批量数据传给下游。

@@snip [groupedWithin](../../../../../cookbook-streams/src/test/scala/cookbook/streams/group/GroupTest.scala) { #groupedWithin }
