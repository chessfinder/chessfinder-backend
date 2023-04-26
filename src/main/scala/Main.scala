package chessfinder

import zio.ZIOApp
import zio.ZIOAppDefault

import sttp.tapir.ztapir.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.http.{ HttpApp, Request, Response }
import zio.*
import zio.http.*
import chessfinder.api.{ AsyncController, SyncController }
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
import chessfinder.client.chess_com.ChessDotComClient
import com.typesafe.config.ConfigFactory
import chessfinder.api.ApiVersion
import chessfinder.search.repo.{ GameRepo, TaskRepo, UserRepo }
import zio.aws.netty
import zio.aws.core.config.AwsConfig
import persistence.core.DefaultDynamoDBExecutor
import zio.dynamodb.*
import zio.logging.backend.SLF4J
import util.EndpointCombiner
import chessfinder.search.TaskStatusChecker
import chessfinder.persistence.GameRecord
import chessfinder.search.GameDownloader
import sttp.tapir.server.ziohttp.*

object Main extends ZIOAppDefault:

  val organization = "eudemonia"

  val syncControllerBlueprint = SyncController("newborn")
  val syncController          = SyncController.Impl(syncControllerBlueprint)

  val asyncControllerBlueprint = AsyncController("async")
  val asyncController          = AsyncController.Impl(asyncControllerBlueprint)

  private val swaggerHost: String = s"http://localhost:8080"

  private val config      = ConfigFactory.load()
  private val configLayer = ZLayer.succeed(config)

  private val dynamodbLayer: TaskLayer[DynamoDBExecutor] =
    val in = ((netty.NettyHttpClient.default >+> AwsConfig.default) ++ configLayer)
    in >>> DefaultDynamoDBExecutor.layer

  private val servers: List[OAServer] = List(OAServer(swaggerHost).description("Admin"))
  private val docsAsYaml: String = OpenAPIDocsInterpreter()
    .toOpenAPI(
      syncControllerBlueprint.endpoints ++ asyncControllerBlueprint.endpoints,
      "ChessFinder",
      "newborn"
    )
    .servers(servers)
    .toYaml

  type AllGameFinders = GameFinder[ApiVersion.Newborn.type] & GameFinder[ApiVersion.Async.type]

  private val zioInterpreter =
    ZioHttpInterpreter[Any](
      ZioHttpServerOptions
        .customiseInterceptors
        .serverLog(
          ZioHttpServerOptions
            .defaultServerLog
            .copy(
              logWhenReceived = true,
              logAllDecodeFailures = true
            )
        )
        .options
    )

  private val swaggerEndpoint: List[ZServerEndpoint[AllGameFinders, Any]] =
    val options = SwaggerUIOptions.default.copy(pathPrefix = List("docs", "swagger"))
    SwaggerUI[zio.RIO[AllGameFinders, *]](docsAsYaml, options = options)

  private val redocEndpoint: List[ZServerEndpoint[AllGameFinders, Any]] =
    val options = RedocUIOptions.default.copy(pathPrefix = List("docs", "redoc"))
    Redoc[zio.RIO[AllGameFinders, *]]("ChessFinder", spec = docsAsYaml, options = options)

  private val rest =
    EndpointCombiner.many(asyncController.rest, syncController.rest)
    // syncController.rest.map(_.widen[AllGameFinders]) ++ asyncController.rest.map(_.widen[AllGameFinders])
  private val endpoints =
    EndpointCombiner.many(EndpointCombiner.many(rest, swaggerEndpoint), redocEndpoint)

  val app =
    zioInterpreter.toHttp(endpoints).withDefaultErrorResponse

  private lazy val clientLayer = Client.default.orDie

  private val logging = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  def run =
    Server
      .serve(app)
      .provide(
        configLayer,
        clientLayer,
        Server.default,
        BoardValidator.Impl.layer,
        GameFinder.Impl.layer[ApiVersion.Newborn.type],
        GameFinder.Impl.layer[ApiVersion.Async.type],
        Searcher.Impl.layer,
        GameFetcher.Impl.layer,
        GameFetcher.Local.layer,
        ChessDotComClient.Impl.layer,
        UserRepo.Impl.layer,
        TaskRepo.Impl.layer,
        GameRepo.Impl.layer,
        TaskStatusChecker.Impl.layer,
        GameDownloader.Impl.layer,
        dynamodbLayer,
        ZLayer.succeed(zio.Random.RandomLive),
        logging
      )
