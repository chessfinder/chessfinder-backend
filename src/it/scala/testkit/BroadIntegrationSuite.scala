package chessfinder
package testkit

import munit.FunSuite

trait BroadIntegrationSuite:

  InitBroadIntegrationEnv.run
  val configLayer = InitBroadIntegrationEnv.configLayer
