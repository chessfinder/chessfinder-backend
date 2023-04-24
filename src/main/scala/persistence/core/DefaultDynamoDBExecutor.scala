package chessfinder
package persistence.core

import com.typesafe.config.{ Config, ConfigFactory }
import io.circe.config.syntax.*
import io.circe.config.*
import zio.*
import zio.aws.dynamodb.DynamoDb
import zio.aws.netty
import zio.aws.core.httpclient.HttpClient
import zio.aws.core.config.AwsConfig
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import zio.dynamodb.*
import persistence.config.DynamoDbConfiguration
import com.typesafe.config.Config

object DefaultDynamoDBExecutor:

  val layer: ZLayer[Config & HttpClient & AwsConfig, Throwable, DynamoDBExecutor] = {
    val dynamoDbConfig: ZLayer[Config, Throwable, DynamoDbConfiguration] =
      ZLayer
        .service[Config]
        .flatMap { rootConfigEnv =>
          val rootConfig: Config = rootConfigEnv.get[Config]
          ZLayer(
            ZIO.attempt(rootConfig.getConfig("database-dynamodb-config").as[DynamoDbConfiguration].toTry.get)
          )
        }
    val cutomDynamoDbLayer = dynamoDbConfig.flatMap { dynamoDbConfigEnv =>
      val dynamoDbConfig: DynamoDbConfiguration = dynamoDbConfigEnv.get[DynamoDbConfiguration]
      DynamoDb.customized { builder =>
        builder
          .endpointOverride(dynamoDbConfig.uriValidated)
          .region(dynamoDbConfig.regionValidated)
          .credentialsProvider(DefaultCredentialsProvider.create())
      }
    }

    val dynamoDbLayer: ZLayer[Config & HttpClient & AwsConfig, Throwable, DynamoDb] =
      ZLayer.service[HttpClient] >>> ZLayer.service[AwsConfig] >>> cutomDynamoDbLayer

    val dynamoDbExecutorLayer: ZLayer[Config & HttpClient & AwsConfig, Throwable, DynamoDBExecutor] =
      (dynamoDbLayer ++ ZLayer.succeed(Clock)) >>> DynamoDBExecutor.live // FIXME for what is the clock? 
    dynamoDbExecutorLayer
  }
