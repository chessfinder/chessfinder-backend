package chessfinder
package client.chess_com

import client.*
import client.ClientError.*
import client.chess_com.ChessDotComClient

import testkit.NarrowIntegrationSuite
import testkit.parser.JsonReader
import testkit.wiremock.ClientBackdoor
import chessfinder.UserName

import com.github.tomakehurst.wiremock.client.WireMock
import com.typesafe.config.ConfigFactory
import sttp.model.Uri
import sttp.model.Uri.UriContext
import zio.*
import zio.http.Client
import zio.test.*

import scala.util.{ Success, Try }

object ChessDotComClientTest extends NarrowIntegrationSuite:
  protected lazy val `chess.com` =
    WireMock.configureFor("localhost", 18443)
    ClientBackdoor("/chess_com")

  protected lazy val env =
    ((Client.default ++ configLayer) >>> ChessDotComClient.Impl.layer).orDie

  def spec =
    suite("ChessDotComClient")(
      suite("profile")(
        test("should get user profile if request is successful") {

          val userName = UserName("tigran-c-1137")

          val stub = `chess.com`
            .expectsEndpoint("GET", "/pub/player/tigran-c-1137")
            .returnsJson(
              """|
                 |{
                 |  "player_id": 191338281,
                 |  "@id": "https://api.chess.com/pub/player/tigran-c-1137",
                 |  "url": "https://www.chess.com/member/tigran-c-1137",
                 |  "username": "tigran-c-1137",
                 |  "followers": 10,
                 |  "country": "https://api.chess.com/pub/country/AM",
                 |  "last_online": 1678264516,
                 |  "joined": 1658920370,
                 |  "status": "premium",
                 |  "is_streamer": false,
                 |  "verified": false,
                 |  "league": "Champion"
                 |}
                 |""".stripMargin
            )
            .stub()

          val expectedResult =
            val uri = uri"https://api.chess.com/pub/player/tigran-c-1137"
            Profile(uri)

          val actualResult = (for {
            _            <- stub
            actualResult <- ZIO.serviceWithZIO[ChessDotComClient](_.profile(userName))
          } yield actualResult).provide(env)

          val stubVerification =
            `chess.com`.verify(1, "GET", "/pub/player/tigran-c-1137")

          assertZIO(actualResult)(Assertion.equalTo(expectedResult)) &&
          assertZIO(stubVerification)(Assertion.isUnit)
        },
        test("should return UserNotFound if it gets 404") {

          val userName = UserName("tigran-c-1138")

          val stub = `chess.com`
            .expectsEndpoint("GET", "/pub/player/tigran-c-1138")
            .returnsStatusCode(404)
            .returnsJson(
              """|{
                 |"code": 0,
                 |"message": "User \"tigran-c-1138\" not found."
                 |}
                 |""".stripMargin
            )
            .stub()

          val actualResult: Call[Profile] = (for {
            _            <- stub.orDie
            actualResult <- ZIO.serviceWithZIO[ChessDotComClient](_.profile(userName))
          } yield actualResult).provide(env)

          val stubVerification =
            `chess.com`.verify(1, "GET", "/pub/player/tigran-c-1138")

          assertZIO(actualResult.exit)(Assertion.fails(Assertion.equalTo(ProfileNotFound(userName)))) &&
          assertZIO(stubVerification)(Assertion.isUnit)
        },
        test("should return SomethingWentWrong in all other cases") {

          val userName = UserName("tigran-c-1139")

          val stub = `chess.com`
            .expectsEndpoint("GET", "/pub/player/tigran-c-1139")
            .returnsStatusCode(429)
            .returnsJson("💣💣💣💣")
            .stub()

          val expectedResult: Call[Profile] = ZIO.fail(SomethingWentWrong)

          val actualResult: Call[Profile] = (for {
            _            <- stub.orDie
            actualResult <- ZIO.serviceWithZIO[ChessDotComClient](_.profile(userName))
          } yield actualResult).provide(env)

          val stubVerification =
            `chess.com`.verify(1, "GET", "/pub/player/tigran-c-1139")

          assertZIO(actualResult.exit)(Assertion.fails(Assertion.equalTo(SomethingWentWrong))) &&
          assertZIO(stubVerification)(Assertion.isUnit)
        }
      ),
      suite("archives")(
        test("should get all archives if request is successful") {

          val userName = UserName("tigran-c-1137")

          val stub = `chess.com`
            .expectsEndpoint("GET", "/pub/player/tigran-c-1137/games/archives")
            .returnsJson(
              """|
                 |{
                 | "archives": [
                 |  "https://pub/player/tigran-c-1137/games/2022/07",
                 |  "https://pub/player/tigran-c-1137/games/2022/08",
                 |  "https://pub/player/tigran-c-1137/games/2022/09",
                 |  "https://pub/player/tigran-c-1137/games/2022/10",
                 |  "https://pub/player/tigran-c-1137/games/2022/11"
                 |  ]
                 |}
                 |""".stripMargin
            )
            .stub()

          val expectedResult =
            Archives(
              Seq(
                uri"https://pub/player/tigran-c-1137/games/2022/07",
                uri"https://pub/player/tigran-c-1137/games/2022/08",
                uri"https://pub/player/tigran-c-1137/games/2022/09",
                uri"https://pub/player/tigran-c-1137/games/2022/10",
                uri"https://pub/player/tigran-c-1137/games/2022/11"
              )
            )

          val actualResult = (for {
            _            <- stub
            actualResult <- ZIO.serviceWithZIO[ChessDotComClient](_.archives(userName))
          } yield actualResult).provide(env)

          val stubVerification =
            `chess.com`.verify(1, "GET", "/pub/player/tigran-c-1137/games/archives")

          assertZIO(actualResult)(Assertion.equalTo(expectedResult)) &&
          assertZIO(stubVerification)(Assertion.isUnit)
        },
        test("should return UserNotFound if it gets 404") {

          val userName = UserName("tigran-c-1138")

          val stub = `chess.com`
            .expectsEndpoint("GET", "/pub/player/tigran-c-1138/games/archives")
            .returnsStatusCode(404)
            .returnsJson(
              """|{
                 |"code": 0,
                 |"message": "User \"tigran-c-1138\" not found."
                 |}
                 |""".stripMargin
            )
            .stub()

          val expectedResult: Call[Archives] = ZIO.fail(ProfileNotFound(userName))

          val actualResult: Call[Archives] = (for {
            _            <- stub.orDie
            actualResult <- ZIO.serviceWithZIO[ChessDotComClient](_.archives(userName))
          } yield actualResult).provide(env)

          val stubVerification =
            `chess.com`.verify(1, "GET", "/pub/player/tigran-c-1138/games/archives")

          assertZIO(actualResult.exit)(Assertion.fails(Assertion.equalTo(ProfileNotFound(userName)))) &&
          assertZIO(stubVerification)(Assertion.isUnit)
        },
        test("should return ServiceIsOverloaded in all other cases") {

          val userName = UserName("tigran-c-1139")

          val stub = `chess.com`
            .expectsEndpoint("GET", "/pub/player/tigran-c-1139/games/archives")
            .returnsStatusCode(429)
            .returnsJson("💣💣💣💣")
            .stub()

          val expectedResult: Call[Archives] = ZIO.fail(SomethingWentWrong)

          val actualResult: Call[Archives] = (for {
            _            <- stub.orDie
            actualResult <- ZIO.serviceWithZIO[ChessDotComClient](_.archives(userName))
          } yield actualResult).provide(env)

          val stubVerification =
            `chess.com`.verify(1, "GET", "/pub/player/tigran-c-1139/games/archives")

          assertZIO(actualResult.exit)(Assertion.fails(Assertion.equalTo(SomethingWentWrong))) &&
          assertZIO(stubVerification)(Assertion.isUnit)
        }
      ),
      suite("games")(
        test("should get a monthly games if request is successful") {

          val resource = uri"/pub/player/tigran-c-1137/games/2022/07"

          val stub = `chess.com`
            .expectsEndpoint("GET", "/pub/player/tigran-c-1137/games/2022/07")
            .returnsJson(
              JsonReader.readResource("samples/2022-07_very_few_games.json")
            )
            .stub()

          val expectedResult =
            val game =
              Game(
                pgn =
                  "[Event \"Live Chess\"]\n[Site \"Chess.com\"]\n[Date \"2022.07.27\"]\n[Round \"-\"]\n[White \"tigran-c-1137\"]\n[Black \"Garevia\"]\n[Result \"1-0\"]\n[CurrentPosition \"r6r/p3Bp1p/4p1k1/3P1RQ1/8/4P3/1P3P1P/4K1NR b K -\"]\n[Timezone \"UTC\"]\n[ECO \"D10\"]\n[ECOUrl \"https://www.chess.com/openings/Slav-Defense-3.Nc3-dxc4-4.e3\"]\n[UTCDate \"2022.07.27\"]\n[UTCTime \"11:18:00\"]\n[WhiteElo \"800\"]\n[BlackElo \"1200\"]\n[TimeControl \"300+5\"]\n[Termination \"tigran-c-1137 won by checkmate\"]\n[StartTime \"11:18:00\"]\n[EndDate \"2022.07.27\"]\n[EndTime \"11:24:30\"]\n[Link \"https://www.chess.com/game/live/52659611873\"]\n\n1. d4 {[%clk 0:05:05]} 1... d5 {[%clk 0:05:05]} 2. c4 {[%clk 0:05:08.9]} 2... c6 {[%clk 0:05:08.5]} 3. Nc3 {[%clk 0:05:03.2]} 3... dxc4 {[%clk 0:05:11.7]} 4. e3 {[%clk 0:05:05.4]} 4... e6 {[%clk 0:05:12.4]} 5. Bxc4 {[%clk 0:05:06.8]} 5... Bb4 {[%clk 0:05:11.5]} 6. Bd2 {[%clk 0:05:05.5]} 6... b5 {[%clk 0:05:12.3]} 7. Be2 {[%clk 0:04:27]} 7... Bxc3 {[%clk 0:05:14.9]} 8. Bxc3 {[%clk 0:04:29.9]} 8... Na6 {[%clk 0:05:06.4]} 9. a4 {[%clk 0:04:32]} 9... Bd7 {[%clk 0:04:56.2]} 10. axb5 {[%clk 0:04:12]} 10... cxb5 {[%clk 0:04:59.7]} 11. Rxa6 {[%clk 0:04:15.5]} 11... Qc8 {[%clk 0:04:53.7]} 12. Ra5 {[%clk 0:04:13]} 12... Qc7 {[%clk 0:04:48.7]} 13. Bxb5 {[%clk 0:03:56.4]} 13... Bxb5 {[%clk 0:04:46]} 14. Rxb5 {[%clk 0:03:59.1]} 14... Qc6 {[%clk 0:04:36.5]} 15. Qa4 {[%clk 0:03:59.6]} 15... Qxg2 {[%clk 0:04:35.7]} 16. Rg5+ {[%clk 0:04:03.7]} 16... Ke7 {[%clk 0:04:27.9]} 17. Rxg2 {[%clk 0:04:02]} 17... Nf6 {[%clk 0:04:29.2]} 18. Rxg7 {[%clk 0:04:04.9]} 18... Ne4 {[%clk 0:04:31.1]} 19. Bb4+ {[%clk 0:03:48.8]} 19... Kf6 {[%clk 0:04:30]} 20. Rg4 {[%clk 0:03:42.2]} 20... Kf5 {[%clk 0:04:27]} 21. Rf4+ {[%clk 0:03:45.7]} 21... Kg5 {[%clk 0:04:21.4]} 22. Be7+ {[%clk 0:03:48.7]} 22... Kg6 {[%clk 0:04:22.8]} 23. d5 {[%clk 0:03:50.3]} 23... Nf6 {[%clk 0:04:19.5]} 24. Rxf6+ {[%clk 0:03:49.2]} 24... Kg5 {[%clk 0:04:20.8]} 25. Qf4+ {[%clk 0:03:51.5]} 25... Kh5 {[%clk 0:04:14.6]} 26. Rf5+ {[%clk 0:03:53]} 26... Kg6 {[%clk 0:04:17.5]} 27. Qg5# {[%clk 0:03:51.1]} 1-0\n",
                url = uri"https://www.chess.com/game/live/52659611873",
                1658921070L
              )
            Games(Seq(game))

          val actualResult = (for {
            _            <- stub
            actualResult <- ZIO.serviceWithZIO[ChessDotComClient](_.games(resource))
          } yield actualResult).provide(env)

          val stubVerification =
            `chess.com`.verify(1, "GET", "/pub/player/tigran-c-1137/games/2022/07")

          assertZIO(actualResult)(Assertion.equalTo(expectedResult)) &&
          assertZIO(stubVerification)(Assertion.isUnit)
        },
        test("should return SomethingWentWrong if it gets other than success") {

          val resource = uri"/pub/player/tigran-c-1138/games/2022/07"

          val stub = `chess.com`
            .expectsEndpoint("GET", "/pub/player/tigran-c-1138/games/2022/07")
            .returnsStatusCode(429)
            .returnsJson("💣💣💣💣")
            .stub()

          val expectedResult: Call[Games] = ZIO.fail(SomethingWentWrong)

          val actualResult: Call[Games] = (for {
            _            <- stub.orDie
            actualResult <- ZIO.serviceWithZIO[ChessDotComClient](_.games(resource))
          } yield actualResult).provide(env)

          val stubVerification =
            `chess.com`.verify(1, "GET", "/pub/player/tigran-c-1138/games/2022/07")

          assertZIO(actualResult.exit)(Assertion.fails(Assertion.equalTo(SomethingWentWrong))) &&
          assertZIO(stubVerification)(Assertion.isUnit)
        }
      )
    ) @@ TestAspect.sequential
