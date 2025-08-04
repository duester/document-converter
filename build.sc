import mill._
import mill.scalalib._
import mill.scalalib.publish._

object `document-converter` extends ScalaModule with PublishModule {
  def scalaVersion: T[String] = "3.7.1"
  def publishVersion = "0.0.1"

  override def ivyDeps: T[Agg[Dep]] = Agg(
    ivy"dev.zio::zio:2.1.20",
    ivy"dev.zio::zio-json:0.7.44"
  )

  object test extends ScalaTests with TestModule.ZioTest {
    override def ivyDeps: T[Agg[Dep]] = Agg(
      ivy"dev.zio::zio-test:2.1.20"
    )
  }

  def pomSettings = PomSettings(
    description = "Converter between different document formats",
    organization = "ru.duester",
    url = "https://github.com/duester/document-converter",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("duester", "document-converter"),
    developers =
      Seq(Developer("duester", "Maxim Duester", "https://github.com/duester"))
  )
} 
