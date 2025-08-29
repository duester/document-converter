//| mill-version: 1.0.3

import mill.*
import mill.scalalib.*
import mill.scalalib.publish.*

object `document-converter` extends ScalaModule, SonatypeCentralPublishModule {
  def scalaVersion = "3.7.1"
  
  def publishVersion = "0.1.4"

  def mvnDeps = Seq(
    mvn"dev.zio::zio:2.1.20",
    mvn"dev.zio::zio-json:0.7.44"
  )

  object test extends ScalaTests, TestModule.ZioTest {
    def mvnDeps = Seq(
      mvn"dev.zio::zio-test:2.1.20"
    )
  }

  def pomSettings = PomSettings(
    description = "Converter between different document formats",
    organization = "io.github.duester",
    url = "https://github.com/duester/document-converter",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("duester", "document-converter"),
    developers =
      Seq(Developer("duester", "Maxim Duester", "https://github.com/duester"))
  )
} 
