package ru.duester.converter

import ru.duester.converter.error.ConversionError
import ru.duester.converter.model.Document
import ru.duester.converter.model.Node
import zio.IO
import zio.Scope
import zio.ZIO
import zio.test.*
import zio.test.ZIOSpecDefault
import zio.test.test
import zio.Exit
import ru.duester.converter.error.ConversionError.FormatError

/*object ConverterSpec extends ZIOSpecDefault:
  type SrcDocument = String
  given srcConverter: FormatConverter[SrcDocument] =
    new FormatConverter[SrcDocument]:
      def fromFormat(
          document: SrcDocument
      ): IO[ConversionError, Document] =
        ZIO.succeed(new Document(List(new Node("text", Some(document)))))

      def toFormat(document: Document): IO[ConversionError, SrcDocument] =
        def toFormatRec(
            nodes: List[Node]
        ): IO[ConversionError, SrcDocument] =
          nodes match {
            case Nil => ZIO.fail(ConversionError.MissingNode("text"))
            case Node("text", Some(content), _, _, _) :: _ =>
              ZIO.succeed(content)
            case _ :: tail => toFormatRec(tail)
          }

        toFormatRec(document.nodes)

  type DestDocument = Int
  given destConverter: FormatConverter[DestDocument] =
    new FormatConverter[DestDocument]:
      def fromFormat(
          document: DestDocument
      ): IO[ConversionError, Document] =
        ZIO.succeed(
          new Document(List(new Node("text", Some(document.toString()))))
        )

      def toFormat(
          document: Document
      ): IO[ConversionError, DestDocument] =
        def toFormatRec(
            nodes: List[Node]
        ): IO[ConversionError, DestDocument] =
          nodes match {
            case Nil => ZIO.fail(ConversionError.MissingNode("text"))
            case Node("text", Some(content), _, _, _) :: _ =>
              content.toIntOption match
                case None =>
                  ZIO.fail(ConversionError.FormatError("text", content))
                case Some(number) => ZIO.succeed(number)
            case _ :: tail => toFormatRec(tail)
          }

        toFormatRec(document.nodes)

  def spec: Spec[TestEnvironment & Scope, Any] =
    suite("test converter")(
      test("success"):
        assertZIO(new DocumentConverter[String, Int].convert("42"))(
          Assertion.equalTo(42)
        )
      ,
      test("FormatError"):
        for {
          exit <- new DocumentConverter[String, Int].convert("a").exit
        } yield assertTrue(exit == Exit.fail(FormatError("text", "a")))
      // TODO more tests
    )*/
