package chessfinder
package download

import search.*
import search.repo.*
import chessfinder.download.details.DownloadStatusResponse

import zio.{ ZIO, ZLayer }

trait DownloadStatusChecker:

  def check(taskId: TaskId): Computation[DownloadStatusResponse]

object DownloadStatusChecker:

  class Impl(taskRepo: TaskRepo) extends DownloadStatusChecker:

    def check(taskId: TaskId): Computation[DownloadStatusResponse] =
      taskRepo.get(taskId).mapError {
        case err: BrokenComputation.TaskNotFound => err
        case _                                   => BrokenComputation.ServiceOverloaded
      }

  object Impl:
    val layer = ZLayer {
      for taskRepo <- ZIO.service[TaskRepo]
      yield Impl(taskRepo)
    }
