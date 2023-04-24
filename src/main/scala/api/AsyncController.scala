package chessfinder
package api

import sttp.tapir.Endpoint
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.Future
import sttp.tapir.json.circe.*
import sttp.tapir.stringBody
import sttp.tapir.ztapir.*
import search.GameFinder
import search.entity.*
import zio.*
import core.SearchFen
import api.TaskResponse
import java.util.UUID
import api.ApiVersion

class AsyncController(val version: String) extends ZTapir:

  private val baseUrl = endpoint.in("api" / version)

  val `POST /api/version/game` =
    baseUrl.post
      .in("game")
      .in(jsonBody[DownloadRequest])
      .out(jsonBody[TaskResponse])
      .errorOut(jsonBody[ApiError])

  val `GET /api/version/board` =
    baseUrl.post
      .in("board")
      .in(jsonBody[FindRequest])
      .out(jsonBody[FindResponse])
      .errorOut(jsonBody[ApiError])

  val `GET /api/version/task` =
    baseUrl.get
      .in("task")
      .in(query[UUID]("taskId"))
      .out(jsonBody[TaskStatusResponse])
      .errorOut(jsonBody[ApiError])

  val `GET /api/version` =
    baseUrl.get
      .out(stringBody)

  lazy val endpoints: List[Endpoint[?, ?, ?, ?, ?]] =
    List(
      `POST /api/version/game`,
      `GET /api/version/board`,
      `GET /api/version/task`,
      `GET /api/version`
    )

object AsyncController:

  type V = ApiVersion.Async.type

  class Impl(blueprint: AsyncController) extends ZTapir:

    val `POST /api/version/game`: ZServerEndpoint[GameFinder[V], Any] =
      def logic(request: DownloadRequest): zio.ZIO[GameFinder[V], ApiError, TaskResponse] = ???
      blueprint.`POST /api/version/game`.zServerLogic(logic)


    val `GET /api/version/board`: ZServerEndpoint[GameFinder[V], Any] =
      def logic(request: FindRequest): zio.ZIO[GameFinder[V], ApiError, FindResponse] = ???
      blueprint.`GET /api/version/board`.zServerLogic(logic)

    val `GET /api/version/task`: ZServerEndpoint[GameFinder[V], Any] =
      def logic(taskId: UUID): zio.ZIO[GameFinder[V], ApiError, TaskStatusResponse] = ???
      blueprint.`GET /api/version/task`.zServerLogic(logic)

      
    val `GET /api/version`: ZServerEndpoint[GameFinder[V], Any] =
      blueprint.`GET /api/version`.zServerLogic(_ => ZIO.succeed(buildinfo.BuildInfo.toString))

    def rest = List(`POST /api/version/game`, `GET /api/version/board`, `GET /api/version/task`, `GET /api/version`)
