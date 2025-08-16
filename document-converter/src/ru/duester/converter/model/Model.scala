package ru.duester.converter.model

import zio.json.DeriveJsonDecoder
import zio.json.DeriveJsonEncoder
import zio.json.JsonDecoder
import zio.json.JsonEncoder

/** Node (a single data block of the document)
  *
  * @param nodeType
  *   type of the node
  * @param children
  *   list of children nodes (if any)
  * @param attributes
  *   map of node attributes (if any)
  */
case class IntermediateNode(
    nodeType: String,
    children: List[IntermediateNode] = Nil,
    attributes: Map[String, String] = Map.empty
)

object IntermediateNode:
  given encoder: JsonEncoder[IntermediateNode] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[IntermediateNode] = DeriveJsonDecoder.gen

/** Structured document representation
  *
  * @param nodes
  *   list of nodes (if any)
  * @param metadata
  *   map of document metadata (if any)
  */
case class IntermediateDocument(
    nodes: List[IntermediateNode] = Nil,
    metadata: Map[String, String] = Map.empty
)

object IntermediateDocument:
  given encoder: JsonEncoder[IntermediateDocument] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[IntermediateDocument] = DeriveJsonDecoder.gen
