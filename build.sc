import mill._
import mill.scalalib._

object `document-converter` extends ScalaModule {
    def scalaVersion: T[String] = "3.7.1"

    override def ivyDeps: T[Agg[Dep]] = Agg(
        ivy"dev.zio::zio:2.1.20",
        ivy"dev.zio::zio-json:0.7.44"
    )

    object test extends ScalaTests with TestModule.ZioTest {
        override def ivyDeps: T[Agg[Dep]] = Agg(
            ivy"dev.zio::zio-test:2.1.20"
        )
    }
}