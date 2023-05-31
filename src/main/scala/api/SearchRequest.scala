package chessfinder
package api

import search.entity.ChessPlatform
import sttp.tapir.Schema

import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}
import zio.json.{DeriveJsonDecoder, JsonDecoder}

import scala.util.Try

final case class FindRequest(
    user: String,
    platform: Platform,
    board: String
)

object FindRequest:
  given Codec[FindRequest]  = deriveCodec[FindRequest]
  given Schema[FindRequest] = Schema.derived[FindRequest]

  given JsonDecoder[FindRequest] = DeriveJsonDecoder.gen[FindRequest]
