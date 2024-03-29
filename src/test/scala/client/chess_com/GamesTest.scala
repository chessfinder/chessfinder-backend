package chessfinder
package client.chess_com

import testkit.parser.JsonReader

import io.circe.{ parser, Decoder }
import munit.*
import munit.Clue.generate
import sttp.model.Uri
import sttp.model.Uri.UriContext

class GamesTest extends FunSuite:
  test("Games should be parsed correctly") {
    val json = parser
      .parse(JsonReader.readResource("samples/2022-07.json"))
      .toTry
      .get

    val expectedResult = Games(
      Seq(
        Game(
          url = uri"https://www.chess.com/game/live/52659611873",
          pgn =
            "[Event \"Live Chess\"]\n[Site \"Chess.com\"]\n[Date \"2022.07.27\"]\n[Round \"-\"]\n[White \"tigran-c-137\"]\n[Black \"Garevia\"]\n[Result \"1-0\"]\n[CurrentPosition \"r6r/p3Bp1p/4p1k1/3P1RQ1/8/4P3/1P3P1P/4K1NR b K -\"]\n[Timezone \"UTC\"]\n[ECO \"D10\"]\n[ECOUrl \"https://www.chess.com/openings/Slav-Defense-3.Nc3-dxc4-4.e3\"]\n[UTCDate \"2022.07.27\"]\n[UTCTime \"11:18:00\"]\n[WhiteElo \"800\"]\n[BlackElo \"1200\"]\n[TimeControl \"300+5\"]\n[Termination \"tigran-c-137 won by checkmate\"]\n[StartTime \"11:18:00\"]\n[EndDate \"2022.07.27\"]\n[EndTime \"11:24:30\"]\n[Link \"https://www.chess.com/game/live/52659611873\"]\n\n1. d4 {[%clk 0:05:05]} 1... d5 {[%clk 0:05:05]} 2. c4 {[%clk 0:05:08.9]} 2... c6 {[%clk 0:05:08.5]} 3. Nc3 {[%clk 0:05:03.2]} 3... dxc4 {[%clk 0:05:11.7]} 4. e3 {[%clk 0:05:05.4]} 4... e6 {[%clk 0:05:12.4]} 5. Bxc4 {[%clk 0:05:06.8]} 5... Bb4 {[%clk 0:05:11.5]} 6. Bd2 {[%clk 0:05:05.5]} 6... b5 {[%clk 0:05:12.3]} 7. Be2 {[%clk 0:04:27]} 7... Bxc3 {[%clk 0:05:14.9]} 8. Bxc3 {[%clk 0:04:29.9]} 8... Na6 {[%clk 0:05:06.4]} 9. a4 {[%clk 0:04:32]} 9... Bd7 {[%clk 0:04:56.2]} 10. axb5 {[%clk 0:04:12]} 10... cxb5 {[%clk 0:04:59.7]} 11. Rxa6 {[%clk 0:04:15.5]} 11... Qc8 {[%clk 0:04:53.7]} 12. Ra5 {[%clk 0:04:13]} 12... Qc7 {[%clk 0:04:48.7]} 13. Bxb5 {[%clk 0:03:56.4]} 13... Bxb5 {[%clk 0:04:46]} 14. Rxb5 {[%clk 0:03:59.1]} 14... Qc6 {[%clk 0:04:36.5]} 15. Qa4 {[%clk 0:03:59.6]} 15... Qxg2 {[%clk 0:04:35.7]} 16. Rg5+ {[%clk 0:04:03.7]} 16... Ke7 {[%clk 0:04:27.9]} 17. Rxg2 {[%clk 0:04:02]} 17... Nf6 {[%clk 0:04:29.2]} 18. Rxg7 {[%clk 0:04:04.9]} 18... Ne4 {[%clk 0:04:31.1]} 19. Bb4+ {[%clk 0:03:48.8]} 19... Kf6 {[%clk 0:04:30]} 20. Rg4 {[%clk 0:03:42.2]} 20... Kf5 {[%clk 0:04:27]} 21. Rf4+ {[%clk 0:03:45.7]} 21... Kg5 {[%clk 0:04:21.4]} 22. Be7+ {[%clk 0:03:48.7]} 22... Kg6 {[%clk 0:04:22.8]} 23. d5 {[%clk 0:03:50.3]} 23... Nf6 {[%clk 0:04:19.5]} 24. Rxf6+ {[%clk 0:03:49.2]} 24... Kg5 {[%clk 0:04:20.8]} 25. Qf4+ {[%clk 0:03:51.5]} 25... Kh5 {[%clk 0:04:14.6]} 26. Rf5+ {[%clk 0:03:53]} 26... Kg6 {[%clk 0:04:17.5]} 27. Qg5# {[%clk 0:03:51.1]} 1-0\n",
          end_time = 1658921070L
        )
      )
    )
    val actualResult = Decoder[Games].decodeJson(json).toTry.get
    assert(expectedResult == actualResult)
  }
