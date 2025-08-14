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
case class Node(
    nodeType: String,
    children: List[Node] = Nil,
    attributes: Map[String, String] = Map.empty
)

object Node:
  given encoder: JsonEncoder[Node] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[Node] = DeriveJsonDecoder.gen

/** Structured document representation
  *
  * @param nodes
  *   list of nodes (if any)
  * @param metadata
  *   map of document metadata (if any)
  */
case class Document(
    nodes: List[Node] = Nil,
    metadata: Map[String, String] = Map.empty
)

object Document:
  given encoder: JsonEncoder[Document] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[Document] = DeriveJsonDecoder.gen
