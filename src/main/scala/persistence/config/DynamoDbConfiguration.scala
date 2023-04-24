package chessfinder
package persistence.config

import software.amazon.awssdk.regions.Region
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.*
import java.net.URI

case class DynamoDbConfiguration(
  region: String,
  uri: String
):
  val uriValidated: URI = URI.create(uri)
  val regionValidated: Region = Region.of(region)

object DynamoDbConfiguration:
  given Decoder[DynamoDbConfiguration] = deriveDecoder[DynamoDbConfiguration]

