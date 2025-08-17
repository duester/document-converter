package ru.duester.converter

import ru.duester.converter.converter.FromIntermediateConverter
import ru.duester.converter.converter.FromIntermediateNodeConverter
import ru.duester.converter.converter.ToIntermediateConverter
import ru.duester.converter.converter.ToIntermediateNodeConverter
import ru.duester.converter.error.ConversionError
import ru.duester.converter.error.ConversionError.FormatError
import ru.duester.converter.model.IntermediateDocument
import ru.duester.converter.model.IntermediateNode
import zio.Exit
import zio.IO
import zio.Scope
import zio.ZIO
import zio.test.*
import zio.test.Assertion.*
import zio.test.ZIOSpecDefault
import zio.test.test

import scala.annotation.tailrec

object ConverterSpec extends ZIOSpecDefault:
  case class MySimpleDocument(
      string: String,
      int: Int
  )

  case class MyNode(
      children: List[MyNode] = Nil,
      attributes: Map[String, String] = Map.empty
  )

  case class MyDocument(
      nodes: List[MyNode] = Nil,
      metadata: Map[String, String] = Map.empty
  )

  object MySimpleDocumentGivens:
    given srcConverter: ToIntermediateConverter[MySimpleDocument]:
      def toIntermediateDocument(
          document: MySimpleDocument
      ): IO[ConversionError, IntermediateDocument] =
        ZIO.succeed(
          IntermediateDocument(
            nodes = List(
              IntermediateNode(
                "mysimple",
                attributes = Map(
                  "string" -> document.string,
                  "int" -> document.int.toString
                )
              )
            )
          )
        )

    given destConverter: FromIntermediateConverter[MySimpleDocument]:
      def fromIntermediateDocument(
          document: IntermediateDocument
      ): IO[ConversionError, MySimpleDocument] =
        @tailrec
        def getMySimpleDocument(
            nodes: List[IntermediateNode]
        ): IO[ConversionError, MySimpleDocument] =
          nodes match
            case Nil => ZIO.fail(ConversionError.MissingNode("mysimple"))
            case IntermediateNode("mysimple", _, attributes) :: _ =>
              (attributes.get("string"), attributes.get("int")) match
                case (Some(string), Some(intString)) =>
                  intString.toIntOption match
                    case None =>
                      ZIO.fail(
                        ConversionError.FormatError(
                          "mysimple",
                          "int",
                          intString
                        )
                      )
                    case Some(int) => ZIO.succeed(MySimpleDocument(string, int))
                case _ =>
                  ZIO.fail(
                    ConversionError.MissingAttributeError(
                      "mysimple",
                      "string/int"
                    )
                  )
            case _ :: tail => getMySimpleDocument(tail)

        getMySimpleDocument(document.nodes)

  object MyDocumentGivens:
    given srcConverter: ToIntermediateNodeConverter[MyDocument, MyNode]:
      def toIntermediateNodes(
          node: MyNode,
          childrenNodes: List[IntermediateNode]
      ): IO[ConversionError, List[IntermediateNode]] =
        ZIO.succeed(
          List(
            IntermediateNode(
              "my",
              children = childrenNodes,
              attributes = node.attributes
            )
          )
        )

      def getNodes(document: MyDocument): List[MyNode] = document.nodes

      def getChildrenNodes(node: MyNode): List[MyNode] = node.children

      def getMetadata(document: MyDocument): Map[String, String] =
        document.metadata

    given destConverter: FromIntermediateNodeConverter[MyDocument, MyNode]:
      def fromIntermediateNodes(
          node: IntermediateNode,
          childrenNodes: List[MyNode]
      ): IO[ConversionError, List[MyNode]] =
        node match
          case IntermediateNode("my", children, attributes) =>
            ZIO.succeed(
              List(
                MyNode(children = childrenNodes, attributes = node.attributes)
              )
            )
          case _ => ZIO.fail(ConversionError.MissingNode("my"))

      def getDocument(
          nodes: List[MyNode],
          metadata: Map[String, String]
      ): IO[ConversionError, MyDocument] =
        nodes match
          case Nil => ZIO.fail(ConversionError.MissingNode("my"))
          case _   => ZIO.succeed(MyDocument(nodes, metadata))

  def spec: Spec[TestEnvironment, Any] =
    import ru.duester.converter.converter.*
    suite("ConverterSpec")(
      test("toIntermediateFull_success"):
        val srcDocument = MySimpleDocument("value", 42)
        for {
          document <- srcDocument.toIntermediateFull(using
            MySimpleDocumentGivens.srcConverter
          )
        } yield assert(document)(
          hasField(
            "nodes",
            (d: IntermediateDocument) => d.nodes,
            hasSize(equalTo(1))
          )
            && hasField(
              "nodeType",
              (d: IntermediateDocument) => d.nodes.head.nodeType,
              equalTo("mysimple")
            )
            && hasField(
              "attributes",
              (d: IntermediateDocument) => d.nodes.head.attributes,
              hasKey("string", equalTo("value"))
                && hasKey("int", equalTo("42"))
            )
        )
      ,
      test("toFull_success"):
        val document = IntermediateDocument(
          List(
            IntermediateNode(
              "mysimple",
              attributes = Map("string" -> "value", "int" -> "42")
            )
          )
        )
        for {
          tgtDocument <- document.toFull(using
            MySimpleDocumentGivens.destConverter
          )
        } yield assert(tgtDocument)(
          hasField(
            "string",
            (d: MySimpleDocument) => d.string,
            equalTo("value")
          )
            && hasField("int", (d: MySimpleDocument) => d.int, equalTo(42))
        )
      ,
      test("toIntermediate_success"):
        val srcDocument = MyDocument(
          List(
            MyNode(
              List(
                MyNode(attributes = Map("key2" -> "value2"))
              ),
              Map("key1" -> "value1")
            )
          ),
          Map("author" -> "somebody")
        )
        for {
          document <- srcDocument.toIntermediate(using
            MyDocumentGivens.srcConverter
          )
        } yield assert(document)(
          hasField(
            "metadata",
            (d: IntermediateDocument) => d.metadata,
            hasKey("author", equalTo("somebody"))
          )
            && hasField(
              "nodes",
              (d: IntermediateDocument) => d.nodes,
              hasSize(equalTo(1))
            )
            && hasField(
              "nodeType",
              (d: IntermediateDocument) => d.nodes.head.nodeType,
              equalTo("my")
            )
            && hasField(
              "attributes",
              (d: IntermediateDocument) => d.nodes.head.attributes,
              hasKey("key1", equalTo("value1"))
            )
            && hasField(
              "children",
              (d: IntermediateDocument) => d.nodes.head.children,
              hasSize(equalTo(1))
            )
            && hasField(
              "nodeType",
              (d: IntermediateDocument) => d.nodes.head.children.head.nodeType,
              equalTo("my")
            )
            && hasField(
              "attributes",
              (d: IntermediateDocument) =>
                d.nodes.head.children.head.attributes,
              hasKey("key2", equalTo("value2"))
            )
            && hasField(
              "children",
              (d: IntermediateDocument) => d.nodes.head.children.head.children,
              isEmpty
            )
        )
      ,
      test("to_success"):
        val document = IntermediateDocument(
          List(
            IntermediateNode(
              "my",
              List(
                IntermediateNode("my", attributes = Map("key2" -> "value2"))
              ),
              Map("key1" -> "value1")
            )
          ),
          Map("author" -> "somebody")
        )
        for {
          tgtDocument <- document.to(using MyDocumentGivens.destConverter)
        } yield assert(tgtDocument)(
          hasField(
            "metadata",
            (d: MyDocument) => d.metadata,
            hasKey("author", equalTo("somebody"))
          )
            && hasField(
              "nodes",
              (d: MyDocument) => d.nodes,
              hasSize(equalTo(1))
            )
            && hasField(
              "attributes",
              (d: MyDocument) => d.nodes.head.attributes,
              hasKey("key1", equalTo("value1"))
            )
            && hasField(
              "children",
              (d: MyDocument) => d.nodes.head.children,
              hasSize(equalTo(1))
            )
            && hasField(
              "attributes",
              (d: MyDocument) => d.nodes.head.children.head.attributes,
              hasKey("key2", equalTo("value2"))
            )
            && hasField(
              "children",
              (d: MyDocument) => d.nodes.head.children.head.children,
              isEmpty
            )
        )
      ,
      test("forth & back_toIntermediateFull_success"):
        val srcDocument = MySimpleDocument("value", 42)
        for {
          document <- srcDocument.toIntermediateFull(using
            MySimpleDocumentGivens.srcConverter
          )
          tgtDocument <- document.toFull(using
            MySimpleDocumentGivens.destConverter
          )
        } yield assert(tgtDocument)(equalTo(srcDocument))
      ,
      test("forth & back_toIntermediate_success"):
        val srcDocument = MyDocument(
          List(
            MyNode(
              List(
                MyNode(attributes = Map("key2" -> "value2"))
              ),
              Map("key1" -> "value1")
            )
          ),
          Map("author" -> "somebody")
        )
        for {
          document <- srcDocument.toIntermediate(using
            MyDocumentGivens.srcConverter
          )
          tgtDocument <- document.to(using MyDocumentGivens.destConverter)
        } yield assert(tgtDocument)(equalTo(srcDocument))
      ,
      test("toFull_failure_MissingNode"):
        val document = IntermediateDocument(Nil)
        for {
          exit <- document
            .toFull(using MySimpleDocumentGivens.destConverter)
            .exit
        } yield assertTrue(
          exit == Exit.fail(ConversionError.MissingNode("mysimple"))
        )
      ,
      test("toFull_failure_MissingAttribute"):
        val document = IntermediateDocument(List(IntermediateNode("mysimple")))
        for {
          exit <- document
            .toFull(using MySimpleDocumentGivens.destConverter)
            .exit
        } yield assertTrue(
          exit == Exit.fail(
            ConversionError.MissingAttributeError("mysimple", "string/int")
          )
        )
      ,
      test("toFull_failure_FormatError"):
        val document = IntermediateDocument(
          List(
            IntermediateNode(
              "mysimple",
              attributes = Map("string" -> "value", "int" -> "not an int")
            )
          )
        )
        for {
          exit <- document
            .toFull(using MySimpleDocumentGivens.destConverter)
            .exit
        } yield assertTrue(
          exit == Exit.fail(
            ConversionError.FormatError("mysimple", "int", "not an int")
          )
        )
      ,
      test("to_failure_MissingNode_noNodes"):
        val document = IntermediateDocument(Nil)
        for {
          exit <- document.to(using MyDocumentGivens.destConverter).exit
        } yield assertTrue(exit == Exit.fail(ConversionError.MissingNode("my")))
      ,
      test(
        "to_failure_MissingNode_wrongNodeType"
      ):
        val document = IntermediateDocument(List(IntermediateNode("wrong")))
        for {
          exit <- document.to(using MyDocumentGivens.destConverter).exit
        } yield assertTrue(exit == Exit.fail(ConversionError.MissingNode("my")))
    )
