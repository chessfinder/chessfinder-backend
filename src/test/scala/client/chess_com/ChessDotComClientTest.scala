package chessfinder
package client.chess_com

import zio.test.*
import zio.*
import client.chess_com.ChessDotComClient
import chessfinder.testkit.wiremock.ClientBackdoor
import sttp.model.Uri
import client.chess_com.dto.*
import client.chess_com.dto.errors.*
import search.entity.UserName
import scala.util.Success

object ChessDotComClientTest extends ZIOSpecDefault with InitFirst:
  protected lazy val `chess.com`               = ClientBackdoor("/chess_com")
  protected lazy val client: ChessDotComClient = ChessDotComClient.Impl()
  def spec =
    suite("ChessDotComClient.profile")(
      test("should get user profile if request is successful") {

        val userName = UserName("tigran-c-137")

        val stub = `chess.com`
          .expectsEndpoint("GET", "/api.chess.com/pub/player/tigran-c-137")
          .returnsJson(
            """|
               |{
               |  "player_id": 191338281,
               |  "@id": "https://api.chess.com/pub/player/tigran-c-137",
               |  "url": "https://www.chess.com/member/tigran-c-137",
               |  "username": "tigran-c-137",
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
          val uri = Uri.parse("https://www.chess.com/member/tigran-c-137").toOption.get
          Profile(uri)

        val actualResult = for {
          _            <- stub
          actualResult <- client.profile(userName)
        } yield actualResult

        val stubVerification =
          `chess.com`.verify(1, "GET", "https://www.chess.com/member/tigran-c-137")

        assertZIO(actualResult)(Assertion.equalTo(expectedResult)) &&
        assertZIO(stubVerification)(Assertion.isUnit)
      },

      test("should return UserNotFound if it gets 404") {

        val userName = UserName("tigran-c-138")

        val stub = `chess.com`
          .expectsEndpoint("GET", "/api.chess.com/pub/player/tigran-c-138")
          .returnsStatusCode(404)
          .returnsJson(
            """|{
               |"code": 0,
               |"message": "User \"tigran-c-138\" not found."
               |}
               |""".stripMargin
          )
          .stub()


        val expectedResult: φ[Profile] = φ.fail(ProfileNotFound(userName))

        val actualResult: φ[Profile] = for {
          _            <- stub.orDie
          actualResult <- client.profile(userName)
        } yield actualResult

        val stubVerification =
          `chess.com`.verify(1, "GET", "https://www.chess.com/member/tigran-c-138")

        assertZIO(actualResult.exit)(Assertion.fails(Assertion.equalTo(ProfileNotFound(userName)))) &&
        assertZIO(stubVerification)(Assertion.isUnit)
      },

      test("should return ServiceIsOverloaded in all other cases") {

        val userName = UserName("tigran-c-139")

        val stub = `chess.com`
          .expectsEndpoint("GET", "/api.chess.com/pub/player/tigran-c-139")
          .returnsStatusCode(429)
          .returnsJson("💣💣💣💣")
          .stub()


        val expectedResult: φ[Profile] = φ.fail(ServiceIsOverloaded)

        val actualResult: φ[Profile] = for {
          _            <- stub.orDie
          actualResult <- client.profile(userName)
        } yield actualResult

        val stubVerification =
          `chess.com`.verify(1, "GET", "https://www.chess.com/member/tigran-c-139")

        assertZIO(actualResult.exit)(Assertion.fails(Assertion.equalTo(ServiceIsOverloaded))) &&
        assertZIO(stubVerification)(Assertion.isUnit)
      },
    ) +
    suite("ChessDotComClient.archives")(

      test("should get all archives if request is successful") {

        val userName = UserName("tigran-c-137")

        val stub = `chess.com`
          .expectsEndpoint("GET", "/api.chess.com/pub/player/tigran-c-137")
          .returnsJson(
            """|
               |{
               |  "player_id": 191338281,
               |  "@id": "https://api.chess.com/pub/player/tigran-c-137",
               |  "url": "https://www.chess.com/member/tigran-c-137",
               |  "username": "tigran-c-137",
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
          val uri = Uri.parse("https://www.chess.com/member/tigran-c-137").toOption.get
          Profile(uri)

        val actualResult = for {
          _            <- stub
          actualResult <- client.profile(userName)
        } yield actualResult

        val stubVerification =
          `chess.com`.verify(1, "GET", "https://www.chess.com/member/tigran-c-137")

        assertZIO(actualResult)(Assertion.equalTo(expectedResult)) &&
        assertZIO(stubVerification)(Assertion.isUnit)
      },

      test("should return UserNotFound if it gets 404") {

        val userName = UserName("tigran-c-138")

        val stub = `chess.com`
          .expectsEndpoint("GET", "/api.chess.com/pub/player/tigran-c-138")
          .returnsStatusCode(404)
          .returnsJson(
            """|{
               |"code": 0,
               |"message": "User \"tigran-c-138\" not found."
               |}
               |""".stripMargin
          )
          .stub()


        val expectedResult: φ[Profile] = φ.fail(ProfileNotFound(userName))

        val actualResult: φ[Profile] = for {
          _            <- stub.orDie
          actualResult <- client.profile(userName)
        } yield actualResult

        val stubVerification =
          `chess.com`.verify(1, "GET", "https://www.chess.com/member/tigran-c-138")

        assertZIO(actualResult.exit)(Assertion.fails(Assertion.equalTo(ProfileNotFound(userName)))) &&
        assertZIO(stubVerification)(Assertion.isUnit)
      },
      
      test("should return ServiceIsOverloaded in all other cases") {

        val userName = UserName("tigran-c-139")

        val stub = `chess.com`
          .expectsEndpoint("GET", "/api.chess.com/pub/player/tigran-c-139")
          .returnsStatusCode(429)
          .returnsJson("💣💣💣💣")
          .stub()


        val expectedResult: φ[Profile] = φ.fail(ServiceIsOverloaded)

        val actualResult: φ[Profile] = for {
          _            <- stub.orDie
          actualResult <- client.profile(userName)
        } yield actualResult

        val stubVerification =
          `chess.com`.verify(1, "GET", "https://www.chess.com/member/tigran-c-139")

        assertZIO(actualResult.exit)(Assertion.fails(Assertion.equalTo(ServiceIsOverloaded))) &&
        assertZIO(stubVerification)(Assertion.isUnit)
      },
    ) +
    suite("ChessDotComClient.games")(
      test("should get a monthly games if request is successful") {

        val userName = UserName("tigran-c-137")

        val stub = `chess.com`
          .expectsEndpoint("GET", "/api.chess.com/pub/player/tigran-c-137")
          .returnsJson(
            """|
               |{
               |  "player_id": 191338281,
               |  "@id": "https://api.chess.com/pub/player/tigran-c-137",
               |  "url": "https://www.chess.com/member/tigran-c-137",
               |  "username": "tigran-c-137",
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
          val uri = Uri.parse("https://www.chess.com/member/tigran-c-137").toOption.get
          Profile(uri)

        val actualResult = for {
          _            <- stub
          actualResult <- client.profile(userName)
        } yield actualResult

        val stubVerification =
          `chess.com`.verify(1, "GET", "https://www.chess.com/member/tigran-c-137")

        assertZIO(actualResult)(Assertion.equalTo(expectedResult)) &&
        assertZIO(stubVerification)(Assertion.isUnit)
      },

      test("should return UserNotFound if it gets 404") {

        val userName = UserName("tigran-c-138")

        val stub = `chess.com`
          .expectsEndpoint("GET", "/api.chess.com/pub/player/tigran-c-138")
          .returnsStatusCode(404)
          .returnsJson(
            """|{
               |"code": 0,
               |"message": "User \"tigran-c-138\" not found."
               |}
               |""".stripMargin
          )
          .stub()


        val expectedResult: φ[Profile] = φ.fail(ProfileNotFound(userName))

        val actualResult: φ[Profile] = for {
          _            <- stub.orDie
          actualResult <- client.profile(userName)
        } yield actualResult

        val stubVerification =
          `chess.com`.verify(1, "GET", "https://www.chess.com/member/tigran-c-138")

        assertZIO(actualResult.exit)(Assertion.fails(Assertion.equalTo(ProfileNotFound(userName)))) &&
        assertZIO(stubVerification)(Assertion.isUnit)
      },
      
      test("should return ServiceIsOverloaded in all other cases") {

        val userName = UserName("tigran-c-139")

        val stub = `chess.com`
          .expectsEndpoint("GET", "/api.chess.com/pub/player/tigran-c-139")
          .returnsStatusCode(429)
          .returnsJson("💣💣💣💣")
          .stub()


        val expectedResult: φ[Profile] = φ.fail(ServiceIsOverloaded)

        val actualResult: φ[Profile] = for {
          _            <- stub.orDie
          actualResult <- client.profile(userName)
        } yield actualResult

        val stubVerification =
          `chess.com`.verify(1, "GET", "https://www.chess.com/member/tigran-c-139")

        assertZIO(actualResult.exit)(Assertion.fails(Assertion.equalTo(ServiceIsOverloaded))) &&
        assertZIO(stubVerification)(Assertion.isUnit)
      }    
    )
