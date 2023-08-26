package chessfinder
package search.repo

import aspect.Span
import persistence.{ PlatformType, UserRecord }
import search.*
import search.*

import zio.dynamodb.{ DynamoDBError, DynamoDBExecutor }
import zio.{ Cause, ZIO, ZLayer }

trait UserRepo:
  def get(user: User): Computation[UserIdentified]

  def save(user: UserIdentified): Computation[Unit]

object UserRepo:

  class Impl(executor: DynamoDBExecutor) extends UserRepo:
    private val layer = ZLayer.succeed(executor)

    override def get(user: User): Computation[UserIdentified] =
      val eff = UserRecord.Table
        .get[UserRecord](user.userName, PlatformType.fromPlatform(user.platform))
        .provideLayer(layer)
        .tapError(e => ZIO.logErrorCause(e.getMessage(), Cause.fail(e)))
        .flatMap(ZIO.fromEither)
        .tapSomeError { case e: DynamoDBError.DecodingError =>
          ZIO.logErrorCause(e.getMessage(), Cause.fail(e))
        }
        .catchNonFatalOrDie {
          case e: DynamoDBError.ValueNotFound => ZIO.fail(BrokenComputation.ProfileIsNotCached(user))
          case _                              => ZIO.fail(BrokenComputation.ServiceOverloaded)
        }
        .map(_.toUser)
      eff @@ Span.log

    override def save(user: UserIdentified): Computation[Unit] =
      val eff = UserRecord.Table
        .put(UserRecord.fromUserIdentified(user))
        .provideLayer(layer)
        .tapError(e => ZIO.logErrorCause(e.getMessage(), Cause.fail(e)))
        .mapError(_ => BrokenComputation.ServiceOverloaded)
      eff @@ Span.log

  object Impl:
    val layer = ZLayer {
      for executor <- ZIO.service[DynamoDBExecutor]
      yield Impl(executor)
    }
