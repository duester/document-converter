package ru.duester.converter.converter

import ru.duester.converter.error.ConversionError
import ru.duester.converter.model.IntermediateDocument
import ru.duester.converter.model.IntermediateNode
import zio.IO
import zio.ZIO

/** Type class for converting document as a whole from source type to
  * intermediate type
  *
  * @tparam TDocument
  *   source document type
  */
trait ToIntermediateConverter[TDocument]:
  /** Converts document from source type to intermediate type
    *
    * @param document
    *   source document
    */
  def toIntermediateDocument(document: TDocument): IO[ConversionError, IntermediateDocument]

/** Type class for converting document as a whole from intermediate type to
  * target type
  *
  * @tparam TDocument
  *   target document type
  */
trait FromIntermediateConverter[TDocument]:
  /** Converts document from intermediate type to target type
    *
    * @param document
    *   intermediate document
    */
  def fromIntermediateDocument(
      document: IntermediateDocument
  ): IO[ConversionError, TDocument]

/** Type class for converting document from source type to intermediate type via
  * (possibly recursive) converting node by node
  *
  * @tparam TDocument
  *   source document type
  * @tparam TNode
  *   source node type
  */
trait ToIntermediateNodeConverter[TDocument, TNode]:
  /** Converts node from source type to intermediate type, given already
    * converted children nodes for this node
    *
    * @param node
    *   source node
    * @param childrenNodes
    *   source children nodes, already converted to intermediate type
    */
  def toIntermediateNodes(
      node: TNode,
      childrenNodes: List[IntermediateNode]
  ): IO[ConversionError, List[IntermediateNode]]

  /** Gets nodes from source document
    *
    * @param document
    *   source document
    */
  def getNodes(document: TDocument): List[TNode]

  /** Gets children nodes from source node
    *
    * @param node
    *   source node
    */
  def getChildrenNodes(node: TNode): List[TNode]

  /** Gets metadata from source document
    *
    * @param document
    *   source document
    */
  def getMetadata(document: TDocument): Map[String, String]

/** Type class for converting document from intermediate type to target type via
  * (possibly recursive) converting node by node
  *
  * @tparam TDocument
  *   target document type
  * @tparam TNode
  *   target node type
  */
trait FromIntermediateNodeConverter[TDocument, TNode]:
  /** Converts node from intermediate type to target type, given already
    * converted children nodes for this node
    *
    * @param node
    *   intermediate node
    * @param childrenNodes
    *   intermediate children nodes, already converted to target type
    */
  def fromIntermediateNodes(
      node: IntermediateNode,
      childrenNodes: List[TNode]
  ): IO[ConversionError, List[TNode]]

  /** Creates target document from nodes and metadata
    *
    * @param nodes
    *   target nodes
    * @param metadata
    *   target metadata
    */
  def getDocument(
      nodes: List[TNode],
      metadata: Map[String, String]
  ): IO[ConversionError, TDocument]

extension [TDocument](d: TDocument)
  /** Converts this document to intermediate type via
    * [[ToIntermediateConverter]]
    *
    * @param converter
    *   converter to use
    */
  def toIntermediate(using
      converter: ToIntermediateConverter[TDocument]
  ): IO[ConversionError, IntermediateDocument] =
    converter.toIntermediateDocument(d)

  /** Converts this document to intermediate type via
    * [[ToIntermediateNodeConverter]]
    *
    * @param converter
    *   converter to use
    * @tparam TNode
    *   type of this document's nodes
    */
  def toIntermediate[TNode](using
      converter: ToIntermediateNodeConverter[TDocument, TNode]
  ): IO[ConversionError, IntermediateDocument] =
    def mapNode(node: TNode): IO[ConversionError, List[IntermediateNode]] =
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
    } yield IntermediateDocument(nodes, converter.getMetadata(d))

extension (d: IntermediateDocument)
  /** Converts this document to target type via [[FromIntermediateConverter]].
    * This method is the most flexible. Use it if the other method with
    * [[FromIntermediateNodeConverter]] is not suitable, e.g. if not all nodes
    * have a common ancestor type.
    *
    * @param converter
    *   converter to use
    */
  def to[TDocument](using
      converter: FromIntermediateConverter[TDocument]
  ): IO[ConversionError, TDocument] =
    converter.fromIntermediateDocument(d)

  /** Converts this document to target type via
    * [[FromIntermediateNodeConverter]]. This method is somewhat simpler to use
    * than the other one with [[FromIntermediateConverter]]. Use it if all nodes
    * have a common ancestor type and possibly a recursive structure.
    *
    * @param converter
    *   converter to use
    * @tparam TNode
    *   type of this document's nodes
    */
  def to[TDocument, TNode](using
      converter: FromIntermediateNodeConverter[TDocument, TNode]
  ): IO[ConversionError, TDocument] =
    def mapNode(node: IntermediateNode): IO[ConversionError, List[TNode]] =
      for {
        childrenTNodesList <- ZIO.collectAll(node.children.map(mapNode))
        childrenTNodes = childrenTNodesList.flatten
        newTNodes <- converter.fromIntermediateNodes(node, childrenTNodes)
      } yield newTNodes

    for {
      tNodesList <- ZIO.collectAll(d.nodes.map(mapNode))
      tNodes = tNodesList.flatten
      document <- converter.getDocument(tNodes, d.metadata)
    } yield document
