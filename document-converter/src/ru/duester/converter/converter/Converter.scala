package ru.duester.converter.converter

import ru.duester.converter.error.ConversionError
import ru.duester.converter.model.Document
import ru.duester.converter.model.Node
import zio.IO
import zio.ZIO

trait ToIntermediateConverter[TDocument]:
  def toIntermediateDocument(document: TDocument): IO[ConversionError, Document]

trait FromIntermediateConverter[TDocument]:
  def fromIntermediateDocument(
      document: Document
  ): IO[ConversionError, TDocument]

trait ToIntermediateNodeConverter[TDocument, TNode]:
  def toIntermediateNodes(
      node: TNode,
      childrenNodes: List[Node]
  ): IO[ConversionError, List[Node]]

  def getNodes(document: TDocument): List[TNode]

  def getChildrenNodes(node: TNode): List[TNode]

  def getMetadata(document: TDocument): Map[String, String]

trait FromIntermediateNodeConverter[TDocument, TNode]:
  def fromIntermediateNodes(
      node: Node,
      childrenNodes: List[TNode]
  ): IO[ConversionError, List[TNode]]

  def getDocument(nodes: List[TNode], metadata: Map[String, String]): TDocument

extension [TDocument](d: TDocument)
  def toIntermediate(using
      converter: ToIntermediateConverter[TDocument]
  ): IO[ConversionError, Document] =
    converter.toIntermediateDocument(d)

  def toIntermediate[TNode](using
      converter: ToIntermediateNodeConverter[TDocument, TNode]
  ): IO[ConversionError, Document] =
    def mapNode(node: TNode): IO[ConversionError, List[Node]] =
      for {
        childreNodesList <- ZIO.collectAll(
          converter.getChildrenNodes(node).map(mapNode)
        )
        childrenNodes = childreNodesList.flatten
        newNodes <- converter.toIntermediateNodes(node, childrenNodes)
      } yield newNodes

    for {
      nodesList <- ZIO.collectAll(converter.getNodes(d).map(mapNode))
      nodes = nodesList.flatten
    } yield Document(nodes, converter.getMetadata(d))

extension (d: Document)
  def to[TDocument](using
      converter: FromIntermediateConverter[TDocument]
  ): IO[ConversionError, TDocument] =
    converter.fromIntermediateDocument(d)

  def to[TDocument, TNode](using
      converter: FromIntermediateNodeConverter[TDocument, TNode]
  ): IO[ConversionError, TDocument] =
    def mapNode(node: Node): IO[ConversionError, List[TNode]] =
      for {
        childrenTNodesList <- ZIO.collectAll(node.children.map(mapNode))
        childrenTNodes = childrenTNodesList.flatten
        newTNodes <- converter.fromIntermediateNodes(node, childrenTNodes)
      } yield newTNodes

    for {
      tNodesList <- ZIO.collectAll(d.nodes.map(mapNode))
      tNodes = tNodesList.flatten
    } yield converter.getDocument(tNodes, d.metadata)
