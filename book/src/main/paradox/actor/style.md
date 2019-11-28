# Typed Actor 风格

本文将讨论各种使用Typed Actor的风格，并尝试着建议一种行之有效的风格作为大家在应用Akka时的参考。

@@@note
任何时候，在准备应用Akka前都应该想一想是否有更简单的方式？如果有，则不要使用Akka！
@@@

## 直接构造 `Behavior`

通过`Behaviors`上的各个便捷函数定义行为，是使用Typed Actor最直观和简单的方式。
```scala
object Hello {
  def apply(): Behavior[Nothing] = Behaviors.receiveMessage { msg =>
    // Do business logic.
    Behaviors.same
  }
}

context.spawn(Hello(), "hello")
```
 
在一个object里通过`apply`定义行为只是一个惯例，你并不需要每次都这样。比如，你可以在`spawn`函数里直接构造一个行为。
```scala
context.spawn(
  Behaviors.receiveMessage { msg => 
    // Do business logic.
    Behaviors.same
  },
  "hello")
``` 

这在构建一个临时actor时有用，但通常这种情况下直接使用`Future`可能会更好。

在object里定义actor行为，object名字相当于对这个行为的所有实例（`spawn`创建以后的`ActorRef[T]`）进行了类型命名（同一业务类型的actor）。同时，在object里定义这个actor能处理的消息是一个不错的对消息进行隔离的地方。

@@snip [Hello](../../../../../cookbook-actor/src/main/scala/cookbook/actor/style/Hello.scala) { #Hello-object }

## 使用函数嵌套

Scala中函数是第一类的，所以我们可以在代码块中直接定义函数，这样就可以将actor的不同行为定义在函数类部。

@@snip [Hello](../../../../../cookbook-actor/src/main/scala/cookbook/actor/style/Hello.scala) { #function }

## 使用类

当actor的逻辑比较复杂时，比如：有多种行为、需要保存状态……函数嵌套的方式使一个函数实现非常的痈肿，代码缩进层次变多……这时，推荐使用类的方式来实现行为（`Behavior`）：

@@snip [Hello](../../../../../cookbook-actor/src/main/scala/cookbook/actor/style/Hello.scala) { #Hello-class }

## 建议

1. 当逻辑比较复杂时，建议使用类的方式来实现actor行为。
2. 函数嵌套的方式若嵌套超过2层或代码行数较大（40行），也建议使用类的方式，通过类成员函数实现行为的切换，而actor状态可以通过函数参数进行传递。

actor状态是作为成员函数的参数进行传递，还是定义成类属性？其实都可以，无所谓好坏，更多的是代码风格问题。

状态作为函数参数传递，优势是可以始终使用不可变数据，这样状态（数据）是线程安全的，可以避免状态的不小修改，且更函数式。但这样的缺点是每次消息处理后你都需要构造一个新的状态集（通常actor内会有多个状态变量或参数）传递给下一个行为，当状态比较多（而大）时这会很繁琐，也会污染正常的业务代码。

状态作为类属性，不可避免的会使用到可变变量或可变数据，这样它们就不是线程安全的了。但是，若你坚持始终在actor消息处理中访问/修改这些状态，那就可以以线程安全的方式处理它们。类属性的方式，不需要每次消息处理完后都构造一个新的状态集，所以某种程度上它不会污染正常的业务代码。
