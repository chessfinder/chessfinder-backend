package chessfinder

import com.github.tomakehurst.wiremock.client.WireMock
import com.typesafe.config.ConfigFactory
import chessfinder.persistence.core.*
import chessfinder.persistence.config.*
import chessfinder.persistence.*
import zio.dynamodb.*
import zio.{ Clock, IO, TaskLayer, ULayer, Unsafe, ZIO, ZLayer }
import scala.util.Try
import scala.concurrent.Await
import scala.concurrent.duration.*
import zio.aws.dynamodb.DynamoDb
import zio.aws.netty
import zio.aws.core.config.AwsConfig
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import com.typesafe.config.{ Config, ConfigFactory }
import io.circe.config.syntax.*
import io.circe.config.*

object InitBroadIntegrationEnv:

  lazy val run =
    setupMock()
    System.setProperty("config.file", "src/it/resources/local.conf")
    setupDynamoDb()
    ConfigFactory.invalidateCaches()
    scala.concurrent.Future(Main.main(Array.empty[String]))(scala.concurrent.ExecutionContext.global)

  def setupMock() = WireMock.configureFor("localhost", 18443)

  def setupDynamoDb() =

    val rootConfigLayer: TaskLayer[Config] = ZLayer.fromZIO(ZIO.attempt(ConfigFactory.load()))

    val dynamodbLayer: TaskLayer[DynamoDBExecutor] =
      val in = ((netty.NettyHttpClient.default >+> AwsConfig.default) ++ rootConfigLayer)
      in >>> DefaultDynamoDBExecutor.layer

    Try {
      val io: IO[Throwable, Unit] =
        val dependentIo =
          for
            _ <- createSortedSetTableWithSingleKey(UserRecord.Table)
            _ <- createSortedSetTableWithSingleKey(GameRecord.Table)
          yield ()
        dependentIo.provide(dynamodbLayer)

      val runtime = zio.Runtime.default
      Await.result(Unsafe.unsafe(implicit unsafe => runtime.unsafe.runToFuture(io)).future, 10.seconds)
    }

  private def createUniqueTableWithSingleKey(
      table: DynamoTable.Unique[?, ?]
  ): ZIO[DynamoDBExecutor, Throwable, Unit] =
    DynamoDBQuery
      .createTable(
        tableName = table.name,
        keySchema = KeySchema(table.partitionKeyName),
        billingMode = BillingMode.PayPerRequest
      )(AttributeDefinition.attrDefnString(table.partitionKeyName))
      .execute

  private def createSortedSetTableWithSingleKey(
      table: DynamoTable.SortedSeq[?, ?, ?]
  ): ZIO[DynamoDBExecutor, Throwable, Unit] =
    DynamoDBQuery
      .createTable(
        tableName = table.name,
        keySchema = KeySchema(table.partitionKeyName, table.sortKeyName),
        billingMode = BillingMode.PayPerRequest
      )(
        AttributeDefinition.attrDefnString(table.partitionKeyName),
        AttributeDefinition.attrDefnString(table.sortKeyName)
      )
      .execute
