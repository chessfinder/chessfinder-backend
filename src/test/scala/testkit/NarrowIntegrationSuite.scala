package chessfinder
package testkit

import munit.FunSuite
import zio.Runtime
import zio.config.typesafe.TypesafeConfigProvider

trait NarrowIntegrationSuite:

  InitNarrowIntegrationEnv.run
  val configLayer = InitNarrowIntegrationEnv.configLayer
