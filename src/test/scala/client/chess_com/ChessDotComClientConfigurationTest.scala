package chessfinder
package client.chess_com

import util.ConfigExtension.*

import munit.*
import sttp.model.Uri
import sttp.model.Uri.UriContext
import zio.*
import zio.config.*
import zio.config.typesafe.*

class ChessDotComClientConfigurationTest extends ZSuite:
  testZ("Profile should be parsed correctly") {
    val confAsString =
      """|{
         |  "client": {
         |    "chesscom": {
         |      "baseUrl": "www.example.com/chess"    
         |    }
         |  }
         |}
      """.stripMargin

    val conf           = TypesafeConfigProvider.fromHoconString(confAsString)
    val expectedResult = ChessDotComClient.Impl.Configuration(uri"www.example.com/chess")
    val configLoaded =
      conf.loadTo[ChessDotComClient.Impl.Configuration]

    for actualResult <- configLoaded
    yield assertEquals(expectedResult, actualResult)

  }
