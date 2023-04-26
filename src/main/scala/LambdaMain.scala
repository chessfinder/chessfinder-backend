package chessfinder

import zio.ZIOApp
import zio.ZIOAppDefault

import sttp.tapir.ztapir.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.http.{ HttpApp, Request, Response }
import zio.*
import zio.http.{ App as _, * }
import chessfinder.search.GameFinder
import zio.Console.ConsoleLive
import sttp.apispec.openapi.Server as OAServer
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.swagger.*
import sttp.tapir.redoc.*
import sttp.tapir.redoc.RedocUIOptions
import sttp.apispec.openapi.circe.yaml.*
import sttp.tapir.server.*
import chessfinder.search.BoardValidator
import chessfinder.search.GameFetcher
import chessfinder.search.Searcher
import chessfinder.search.TaskStatusChecker
import chessfinder.search.GameDownloader
import chessfinder.client.chess_com.ChessDotComClient
import com.typesafe.config.ConfigFactory
import sttp.tapir.serverless.aws.lambda.LambdaHandler

import cats.effect.unsafe.implicits.global
import com.amazonaws.services.lambda.runtime.Context
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.serverless.aws.lambda.{ AwsRequest, LambdaHandler }
import java.io.{ InputStream, OutputStream }
import cats.implicits.*
import sttp.tapir.serverless.aws.lambda.zio.ZLambdaHandler
import zio.Task
import zio.{ Task, ZIO }
import cats.effect.unsafe.implicits.global
import com.amazonaws.services.lambda.runtime.Context
import io.circe.generic.auto.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.serverless.aws.lambda.{ AwsRequest, LambdaHandler }
import java.io.{ InputStream, OutputStream }
import sttp.tapir.serverless.aws.lambda.zio.ZLambdaHandler
import sttp.tapir.ztapir.ZServerEndpoint
import sttp.tapir.ztapir.RIOMonadError
import zio.{ Runtime, Unsafe }
import chessfinder.api.{ AsyncController, SyncController }
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import zio.logging.*
import chessfinder.client.ZLoggingAspect
import zio.logging.backend.SLF4J
import chessfinder.api.ApiVersion
import chessfinder.search.repo.{ GameRepo, TaskRepo, UserRepo }
import zio.aws.netty
import zio.aws.core.config.AwsConfig
import persistence.core.DefaultDynamoDBExecutor
import zio.dynamodb.*
import util.EndpointCombiner

object LambdaMain extends RequestStreamHandler:

  val organization = "eudemonia"

  val syncController  = SyncController.Impl(SyncController("newborn"))
  val asyncController = AsyncController.Impl(AsyncController("async"))

  type AllGameFinders = GameFinder[ApiVersion.Newborn.type] & GameFinder[ApiVersion.Async.type]
  val handler = ZLambdaHandler.withMonadError(
    EndpointCombiner.many(syncController.rest, asyncController.rest)
  )

  private val config      = ConfigFactory.load()
  private val configLayer = ZLayer.succeed(config)

  private val dynamodbLayer: TaskLayer[DynamoDBExecutor] =
    val in = ((netty.NettyHttpClient.default >+> AwsConfig.default) ++ configLayer)
    in >>> DefaultDynamoDBExecutor.layer

  private lazy val clientLayer =
    Client.default.map(z => z.update(_ @@ ZLoggingAspect())).orDie

  def process(input: InputStream, output: OutputStream) =
    val logging = Runtime.removeDefaultLoggers >>> SLF4J.slf4j
    handler
      .process[AwsRequest](input, output)
      .provide(
        configLayer,
        clientLayer,
        BoardValidator.Impl.layer,
        GameFinder.Impl.layer[ApiVersion.Newborn.type],
        GameFinder.Impl.layer[ApiVersion.Async.type],
        Searcher.Impl.layer,
        GameFetcher.Impl.layer,
        GameFetcher.Local.layer,
        ChessDotComClient.Impl.layer,
        UserRepo.Impl.layer,
        GameRepo.Impl.layer,
        TaskRepo.Impl.layer,
        TaskStatusChecker.Impl.layer,
        GameDownloader.Impl.layer,
        dynamodbLayer,
        ZLayer.succeed(zio.Random.RandomLive),
        logging
      )

  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit =
    val runtime = Runtime.default
    Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.run(process(input, output)).getOrThrowFiberFailure()
    }
