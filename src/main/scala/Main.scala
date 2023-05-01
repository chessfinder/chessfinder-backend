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
import zio.logging.*
import zio.config.typesafe.TypesafeConfigProvider

object Main extends ZIOAppDefault:

  val organization = "eudemonia"

  private val configLayer = Runtime.setConfigProvider(TypesafeConfigProvider.fromResourcePath())
  // private val loggingLayer = Runtime.removeDefaultLoggers >>> SLF4J.slf4j
  private val loggingLayer = Runtime.removeDefaultLoggers >>> zio.logging.consoleJsonLogger()

  val syncControllerBlueprint = SyncController("newborn")
  val syncController          = SyncController.Impl(syncControllerBlueprint)

  val asyncControllerBlueprint = AsyncController("async")
  val asyncController          = AsyncController.Impl(asyncControllerBlueprint)

  private val dynamodbLayer: TaskLayer[DynamoDBExecutor] =
    val in = ((netty.NettyHttpClient.default >+> AwsConfig.default) ++ configLayer)
    in >>> DefaultDynamoDBExecutor.layer

  private val servers: List[OAServer] = List(
    OAServer("http://localhost:8080").description("Chessfinder APIs")
  )
  private val docsAsYaml: String = OpenAPIDocsInterpreter()
    .toOpenAPI(
      syncControllerBlueprint.endpoints ++ asyncControllerBlueprint.endpoints,
      "ChessFinder",
      "Backend"
    )
    .servers(servers)
    .toYaml

  private val zioInterpreter =
    ZioHttpInterpreter[Any](
      ZioHttpServerOptions.customiseInterceptors
        .serverLog(
          ZioHttpServerOptions.defaultServerLog
            .copy(
              doLogWhenReceived = msg => ZIO.logInfo(msg),
              doLogWhenHandled = (msg: String, exOpt: Option[Throwable]) =>
                ZIO.logInfoCause(msg, exOpt.map(e => Cause.fail(e)).getOrElse(Cause.empty)),
              doLogAllDecodeFailures = (msg: String, exOpt: Option[Throwable]) =>
                ZIO.logInfoCause(msg, exOpt.map(e => Cause.fail(e)).getOrElse(Cause.empty)),
              doLogExceptions = (msg: String, ex: Throwable) => ZIO.logErrorCause(msg, Cause.fail(ex)),
              noLog = ZIO.unit,
              logWhenReceived = true,
              logAllDecodeFailures = true
            )
        )
        .options
    )

  private val swaggerEndpoint =
    val options = SwaggerUIOptions.default.copy(pathPrefix = List("docs", "swagger"))
    SwaggerUI[zio.RIO[Any, *]](docsAsYaml, options = options)

  private val redocEndpoint =
    val options = RedocUIOptions.default.copy(pathPrefix = List("docs", "redoc"))
    Redoc[zio.RIO[Any, *]]("ChessFinder", spec = docsAsYaml, options = options)

  private val rest =
    EndpointCombiner.many(asyncController.rest, syncController.rest)

  private val endpoints =
    EndpointCombiner.many(EndpointCombiner.many(rest, swaggerEndpoint), redocEndpoint)

  val app =
    zioInterpreter.toHttp(endpoints).withDefaultErrorResponse

  private lazy val clientLayer = Client.default.orDie

  override val bootstrap = configLayer >+> loggingLayer

  ZIOAspect
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
        ZLayer.succeed(zio.Random.RandomLive)
      )
