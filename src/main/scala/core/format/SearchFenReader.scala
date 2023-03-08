package chessfinder
package core.format

import cats.implicits.*
import cats.kernel.Monoid
import ornicar.scalalib.zeros.given_Zero_Option
import core.{ ProbabilisticBoard, ProbabilisticPiece }
import core.error.β
import core.error.βExt.*
import chess.Pos

object SearchFenReader:
  def read(fen: SearchFen): β[ProbabilisticBoard] =
    val positionOrError =
      val word = fen.value.trim().takeWhile(' ' !=)
      if
        val rows = word.split('/')
        word.count('/' ==) == 7 && rows.forall(_.length() == 8)
      then word.validated
      else s"FEN $fen is not valid".failed
    positionOrError.map(positions =>
      val pieces = makePieces(List.empty)(positions.toList, 0, 7)
      ProbabilisticBoard.fromMap(pieces.toMap)
    )

  @scala.annotation.tailrec
  private def makePieces(acc: List[(Pos, ProbabilisticPiece)])(
      chars: List[Char],
      x: Int,
      y: Int
  ): List[(Pos, ProbabilisticPiece)] =
    chars match
      case Nil         => acc
      case '/' :: rest => makePieces(acc)(rest, 0, y - 1)
      case '-' :: rest => makePieces(acc)(rest, x + 1, y)
      case c :: rest =>
        val newAcc = for {
          pos   <- Pos.at(x, y)
          piece <- ProbabilisticPiece.fromChar(c)
          elem   = pos -> piece
          newAcc = elem :: acc
        } yield newAcc
        makePieces(newAcc.getOrElse(acc))(rest, x + 1, y)
