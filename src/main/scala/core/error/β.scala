package chessfinder
package core.error

import cats.data.{ NonEmptyChain, Validated }
import scala.util.{ Failure, Success, Try }
import scala.util.control.NonFatal

type β[T] = Validated[ValidationErrors, T]

object β:

  def valid[T](v: T): β[T] =
    Validated.validNec[ValidationError, T](v)

  def invalid[T](e: ValidationError): β[T] =
    Validated.invalidNec[ValidationError, T](e)

  def fromOption[T](
      option: Option[T],
      ifNone: => ValidationError
  ): β[T] = Validated.fromOption(option, ifNone).toValidatedNec

  def fromEither[T](either: Either[ValidationError, T]): β[T] =
    Validated.fromEither(either).toValidatedNec

  def fromTry[T](tr: Try[T]): β[T] =
    tr match
      case Success(value) => Validated.validNec[ValidationError, T](value)
      case Failure(NonFatal(exception)) =>
        Validated.invalidNec[ValidationError, T](exception.getMessage)
      case Failure(exception) => throw exception

  def fromValidated[T](validated: Validated[ValidationError, T]): β[T] =
    validated.toValidatedNec

  def catchNonfatal[T](effect: => T): β[T] =
    fromTry(Try(effect))

object βExt:

  extension [OUTPUT](out: OUTPUT)
    def validated: β[OUTPUT] =
      β.valid[OUTPUT](out)

  extension (error: ValidationError)
    def failed[OUTPUT]: β[OUTPUT] =
      β.invalid[OUTPUT](error)
