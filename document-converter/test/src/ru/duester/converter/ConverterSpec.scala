package ru.duester.converter

import ru.duester.converter.converter.FromIntermediateConverter
import ru.duester.converter.converter.FromIntermediateNodeConverter
import ru.duester.converter.converter.ToIntermediateConverter
import ru.duester.converter.converter.ToIntermediateNodeConverter
import ru.duester.converter.error.ConversionError
import ru.duester.converter.error.ConversionError.FormatError
import ru.duester.converter.model.Document
import ru.duester.converter.model.Node
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
      ): IO[ConversionError, Document] =
        ZIO.succeed(
          Document(
            nodes = List(
              Node(
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
          document: Document
      ): IO[ConversionError, MySimpleDocument] =
        @tailrec
        def getMySimpleDocument(
            nodes: List[Node]
        ): IO[ConversionError, MySimpleDocument] =
          nodes match
            case Nil => ZIO.fail(ConversionError.MissingNode("mysimple"))
            case Node("mysimple", _, attributes) :: _ =>
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
          childrenNodes: List[Node]
      ): IO[ConversionError, List[Node]] =
        ZIO.succeed(
          List(
            Node("my", children = childrenNodes, attributes = node.attributes)
          )
        )

      def getNodes(document: MyDocument): List[MyNode] = document.nodes

      def getChildrenNodes(node: MyNode): List[MyNode] = node.children

      def getMetadata(document: MyDocument): Map[String, String] =
        document.metadata

    given destConverter: FromIntermediateNodeConverter[MyDocument, MyNode]:
      def fromIntermediateNodes(
          node: Node,
          childrenNodes: List[MyNode]
      ): IO[ConversionError, List[MyNode]] =
        node match
          case Node("my", children, attributes) =>
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
      test("toIntermediate_ToIntermediateConverter_success"):
        val srcDocument = MySimpleDocument("value", 42)
        for {
          document <- srcDocument.toIntermediate(using
            MySimpleDocumentGivens.srcConverter
          )
        } yield assert(document)(
          hasField("nodes", (d: Document) => d.nodes, hasSize(equalTo(1)))
            && hasField(
              "nodeType",
              (d: Document) => d.nodes.head.nodeType,
              equalTo("mysimple")
            )
            && hasField(
              "attributes",
              (d: Document) => d.nodes.head.attributes,
              hasKey("string", equalTo("value"))
                && hasKey("int", equalTo("42"))
            )
        )
      ,
      test("to_FromIntermediateConverter_success"):
        val document = Document(
          List(
            Node(
              "mysimple",
              attributes = Map("string" -> "value", "int" -> "42")
            )
          )
        )
        for {
          tgtDocument <- document.to(using MySimpleDocumentGivens.destConverter)
        } yield assert(tgtDocument)(
          hasField(
            "string",
            (d: MySimpleDocument) => d.string,
            equalTo("value")
          )
            && hasField("int", (d: MySimpleDocument) => d.int, equalTo(42))
        )
      ,
      test("toIntermediate_ToIntermediateNodeConverter_success"):
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
            (d: Document) => d.metadata,
            hasKey("author", equalTo("somebody"))
          )
            && hasField("nodes", (d: Document) => d.nodes, hasSize(equalTo(1)))
            && hasField(
              "nodeType",
              (d: Document) => d.nodes.head.nodeType,
              equalTo("my")
            )
            && hasField(
              "attributes",
              (d: Document) => d.nodes.head.attributes,
              hasKey("key1", equalTo("value1"))
            )
            && hasField(
              "children",
              (d: Document) => d.nodes.head.children,
              hasSize(equalTo(1))
            )
            && hasField(
              "nodeType",
              (d: Document) => d.nodes.head.children.head.nodeType,
              equalTo("my")
            )
            && hasField(
              "attributes",
              (d: Document) => d.nodes.head.children.head.attributes,
              hasKey("key2", equalTo("value2"))
            )
            && hasField(
              "children",
              (d: Document) => d.nodes.head.children.head.children,
              isEmpty
            )
        )
      ,
      test("to_FromIntermediateNodeConverter_success"):
        val document = Document(
          List(
            Node(
              "my",
              List(Node("my", attributes = Map("key2" -> "value2"))),
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
      test("forth & back_ToIntermediateConverter_success"):
        val srcDocument = MySimpleDocument("value", 42)
        for {
          document <- srcDocument.toIntermediate(using
            MySimpleDocumentGivens.srcConverter
          )
          tgtDocument <- document.to(using MySimpleDocumentGivens.destConverter)
        } yield assert(tgtDocument)(equalTo(srcDocument))
      ,
      test("forth & back_ToIntermediateNodeConverter_success"):
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
      test("to_FromIntermediateConverter_failure_MissingNode"):
        val document = Document(Nil)
        for {
          exit <- document.to(using MySimpleDocumentGivens.destConverter).exit
        } yield assertTrue(
          exit == Exit.fail(ConversionError.MissingNode("mysimple"))
        )
      ,
      test("to_FromIntermediateConverter_failure_MissingAttribute"):
        val document = Document(List(Node("mysimple")))
        for {
          exit <- document.to(using MySimpleDocumentGivens.destConverter).exit
        } yield assertTrue(
          exit == Exit.fail(
            ConversionError.MissingAttributeError("mysimple", "string/int")
          )
        )
      ,
      test("to_FromIntermediateConverter_failure_FormatError"):
        val document = Document(
          List(
            Node(
              "mysimple",
              attributes = Map("string" -> "value", "int" -> "not an int")
            )
          )
        )
        for {
          exit <- document.to(using MySimpleDocumentGivens.destConverter).exit
        } yield assertTrue(
          exit == Exit.fail(
            ConversionError.FormatError("mysimple", "int", "not an int")
          )
        )
      ,
      test("to_FromIntermediateNodeConverter_failure_MissingNode_noNodes"):
        val document = Document(Nil)
        for {
          exit <- document.to(using MyDocumentGivens.destConverter).exit
        } yield assertTrue(exit == Exit.fail(ConversionError.MissingNode("my")))
      ,
      test(
        "to_FromIntermediateNodeConverter_failure_MissingNode_wrongNodeType"
      ):
        val document = Document(List(Node("wrong")))
        for {
          exit <- document.to(using MyDocumentGivens.destConverter).exit
        } yield assertTrue(exit == Exit.fail(ConversionError.MissingNode("my")))
    )
