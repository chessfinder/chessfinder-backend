package chessfinder
package client

import chessfinder.UserName
import sttp.model.Uri
import zio.{ IO, ZIO }

trait ClientError(val msg: String)

object ClientError:
  case class ProfileNotFound(userName: UserName) extends ClientError(s"Profile $userName not found!")
  case class ArchiveNotFound(resource: Uri)      extends ClientError(s"Archive $resource not found!")
  case object SomethingWentWrong                 extends ClientError("Something went wrong!")

type Call[T] = IO[ClientError, T]
