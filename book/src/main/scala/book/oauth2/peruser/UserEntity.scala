/*
 * Copyright 2019 yangbajing.me
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package book.oauth2.peruser

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, TimerScheduler }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityContext, EntityTypeKey }
import akka.cluster.sharding.typed.{ ClusterShardingSettings, ShardingEnvelope }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import book.oauth2.OAuthUtils.DueEpochMillis
import book.oauth2.{ AccessToken, OAuthUtils }
import fusion.json.CborSerializable

import scala.concurrent.duration._

// #UserEntity
object UserEntity {
  final case class State(
      tokens: Map[String, DueEpochMillis] = Map(),
      refreshTokens: Map[String, DueEpochMillis] = Map())
      extends CborSerializable {
    def clear(clearTokens: IterableOnce[String], clearRefreshTokens: IterableOnce[String]): State =
      copy(tokens = tokens -- clearTokens, refreshTokens = refreshTokens -- clearRefreshTokens)

    def addToken(created: TokenCreated): State = {
      val tokenDue = OAuthUtils.expiresInToEpochMillis(created.accessTokenExpiresIn)
      val refreshTokenDue = OAuthUtils.expiresInToEpochMillis(created.refreshTokenExpiresIn)
      State(tokens + (created.accessToken -> tokenDue), refreshTokens + (created.refreshToken -> refreshTokenDue))
    }

    def addToken(accessToken: String, expiresIn: FiniteDuration): State =
      copy(tokens = tokens + (accessToken -> OAuthUtils.expiresInToEpochMillis(expiresIn)))
  }

  sealed trait Command extends CborSerializable

  final case class CreateToken(replyTo: ActorRef[AccessToken]) extends Command
  final case class CheckToken(accessToken: String, replyTo: ActorRef[Int]) extends Command
  final case class RefreshToken(refreshToken: String, replyTo: ActorRef[Option[AccessToken]]) extends Command
  final case object ClearTick extends Command

  sealed trait Event extends CborSerializable
  final case class TokenCreated(
      accessToken: String,
      accessTokenExpiresIn: FiniteDuration,
      refreshToken: String,
      refreshTokenExpiresIn: FiniteDuration)
      extends Event
  final case class TokenRefreshed(accessToken: String, expiresIn: FiniteDuration) extends Event
  final case class ClearEvent(clearTokens: Set[String], clearRefreshTokens: Set[String]) extends Event

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey("UserEntity")

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(
      Entity(TypeKey)(ec => apply(ec))
        .withSettings(ClusterShardingSettings(system).withPassivateIdleEntityAfter(Duration.Zero)))

  private def apply(ec: EntityContext[Command]): Behavior[Command] = {
    val userId = ec.entityId
    Behaviors.setup(
      context =>
        Behaviors.withTimers(timers =>
          new UserEntity(PersistenceId.of(ec.entityTypeKey.name, ec.entityId), userId, timers, context)
            .eventSourcedBehavior()))
  }
}

import book.oauth2.peruser.UserEntity._
class UserEntity private (
    persistenceId: PersistenceId,
    userId: String,
    timers: TimerScheduler[Command],
    context: ActorContext[Command]) {
  timers.startTimerWithFixedDelay(ClearTick, 2.hours)

  def eventSourcedBehavior(): EventSourcedBehavior[Command, Event, State] =
    EventSourcedBehavior(
      persistenceId,
      State(),
      (state, command) =>
        command match {
          case CheckToken(accessToken, replyTo)    => processCheckToken(state, accessToken, replyTo)
          case RefreshToken(refreshToken, replyTo) => processRefreshToken(state, refreshToken, replyTo)
          case CreateToken(replyTo)                => processCreateToken(replyTo)
          case ClearTick                           => processClear(state)
        },
      (state, event) =>
        event match {
          case TokenRefreshed(accessToken, expiresIn)      => state.addToken(accessToken, expiresIn)
          case created: TokenCreated                       => state.addToken(created)
          case ClearEvent(clearTokens, clearRefreshTokens) => state.clear(clearTokens, clearRefreshTokens)
        })

  private def processRefreshToken(
      state: State,
      refreshToken: String,
      replyTo: ActorRef[Option[AccessToken]]): Effect[Event, State] = {
    if (state.refreshTokens.get(refreshToken).exists(due => System.currentTimeMillis() < due)) {
      val refreshed = TokenRefreshed(OAuthUtils.generateToken(userId), 2.hours)
      Effect
        .persist(refreshed)
        .thenReply(replyTo)(_ => Some(AccessToken(refreshed.accessToken, refreshed.expiresIn.toSeconds, refreshToken)))
    } else {
      Effect.reply(replyTo)(None)
    }
  }

  private def processCheckToken(state: State, accessToken: String, replyTo: ActorRef[Int]): Effect[Event, State] = {
    val status = state.tokens.get(accessToken) match {
      case Some(dueTimestamp) => if (System.currentTimeMillis() < dueTimestamp) 200 else 401
      case None               => 401
    }
    Effect.reply(replyTo)(status)
  }

  private def processCreateToken(replyTo: ActorRef[AccessToken]): Effect[Event, State] = {
    val createdEvent =
      TokenCreated(OAuthUtils.generateToken(userId), 2.hours, OAuthUtils.generateToken(userId), 30.days)
    Effect.persist(createdEvent).thenReply(replyTo) { _ =>
      AccessToken(createdEvent.accessToken, createdEvent.accessTokenExpiresIn.toSeconds, createdEvent.refreshToken)
    }
  }

  private def processClear(state: State): Effect[Event, State] = {
    if (state.tokens.isEmpty && state.refreshTokens.isEmpty) {
      Effect.stop()
    } else {
      val now = System.currentTimeMillis()
      val clearTokens = state.tokens.view.filterNot { case (_, due)               => now < due }.keys.toSet
      val clearRefreshTokens = state.refreshTokens.view.filterNot { case (_, due) => now < due }.keys.toSet
      Effect.persist(ClearEvent(clearTokens, clearRefreshTokens))
    }
  }
}
// #UserEntity
