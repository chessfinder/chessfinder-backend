package chessfinder
package api

import search.entity.{DownloadStatus, SearchResult, SearchStatus}
import sttp.model.Uri
import sttp.tapir.Schema

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import zio.json.*

final case class FindResponse(
    resources: Seq[Uri],
    message: String
)

object FindResponse:
  import util.UriCodec.given

  given Codec[FindResponse]  = deriveCodec[FindResponse]
  given Schema[FindResponse] = Schema.derived[FindResponse]

  given JsonEncoder[FindResponse] = DeriveJsonEncoder.gen[FindResponse]

  def fromSearchResult(result: SearchResult): FindResponse = ???
    // val message = result.status match
    //   case SearchStatus.SearchedAll => "All games are successfully analized."
    //   case SearchStatus.SearchedPartially    => "Not all games are analized."
    //   case SearchStatus.InProgress    => "Not all games are analized."
      
    // FindResponse(resources = result.matched.map(_.resource), message = message)
