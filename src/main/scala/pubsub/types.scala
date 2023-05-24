package chessfinder
package pubsub

import io.circe.{ Codec, Decoder, Encoder }
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema
import search.entity.ChessPlatform
import scala.util.Try
import zio.json.{ DeriveJsonDecoder, JsonDecoder }

enum Platform:
  case CHESS_DOT_COM
  case LICHESS

  def toPlatform = this match
    case CHESS_DOT_COM => ChessPlatform.ChessDotCom
    case LICHESS       => ChessPlatform.Lichess

object Platform:

  def fromPlatform(p: ChessPlatform) = p match
    case ChessPlatform.ChessDotCom => CHESS_DOT_COM
    case ChessPlatform.Lichess     => LICHESS

  private val encoder   = Encoder[String].contramap[Platform](_.toString())
  private val decoder   = Decoder[String].emapTry[Platform](str => Try(Platform.valueOf(str)))
  given Codec[Platform] = Codec.from[Platform](decoder, encoder)
  // given JsonDecoder[Platform] =
  //   JsonDecoder[String].mapOrFail(s => Try(Platform.valueOf(s)).toEither.left.map(_.getMessage()))
