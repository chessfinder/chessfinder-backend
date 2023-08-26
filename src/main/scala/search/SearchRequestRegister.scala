package chessfinder
package search

import core.SearchFen
import BrokenComputation.NoGameAvailable
import search.repo.{ SearchResultRepo, UserRepo }
import chessfinder.download.details.ArchiveRepo

import chessfinder.search.details.SearchBoardCommandPublisher

import zio.{ Clock, Random, ZIO, ZLayer }

trait SearchRequestRegister:

  def register(board: SearchFen, platform: ChessPlatform, userName: UserName): Computation[SearchResult]

object SearchRequestRegister:

  class Impl(
      validator: BoardValidator,
      boardSearchingProducer: SearchBoardCommandPublisher,
      userRepo: UserRepo,
      archiveRepo: ArchiveRepo,
      searchResultRepo: SearchResultRepo,
      clock: Clock,
      random: Random
  ) extends SearchRequestRegister:

    def register(board: SearchFen, platform: ChessPlatform, userName: UserName): Computation[SearchResult] =
      for
        _ <- validator.validate(board)
        user = User(platform, userName)
        userIdentified <- userRepo.get(user)
        archives       <- archiveRepo.getAll(userIdentified.userId)
        totalGames = archives.map(_.downloaded).sum
        _               <- ZIO.cond(totalGames > 0, (), NoGameAvailable(user))
        now             <- clock.instant
        searchRequestId <- random.nextUUID
        searchResult    <- searchResultRepo.initiate(SearchRequestId(searchRequestId), now, totalGames)
        _               <- boardSearchingProducer.publish(userIdentified, board, searchResult.id)
      yield searchResult


  object Impl:
    def layer = ZLayer {
      for
        validator              <- ZIO.service[BoardValidator]
        boardSearchingProducer <- ZIO.service[SearchBoardCommandPublisher]
        userRepo               <- ZIO.service[UserRepo]
        archiveRepo            <- ZIO.service[ArchiveRepo]
        searchResultRepo       <- ZIO.service[SearchResultRepo]
        clock                  <- ZIO.service[Clock]
        random                 <- ZIO.service[Random]
      yield Impl(validator, boardSearchingProducer, userRepo, archiveRepo, searchResultRepo, clock, random)
    }
