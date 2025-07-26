package ru.duester.converter.model

import zio.json.DeriveJsonDecoder
import zio.json.DeriveJsonEncoder
import zio.json.JsonDecoder
import zio.json.JsonEncoder

case class Node(
    nodeType: String,
    textContent: Option[String] = None,
    binaryContent: Option[Array[Byte]] = None,
    children: List[Node] = Nil,
    attributes: Map[String, String] = Map.empty
)

object Node:
  given encoder: JsonEncoder[Node] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[Node] = DeriveJsonDecoder.gen

case class Document(
    nodes: List[Node] = Nil,
    metadata: Map[String, String] = Map.empty
)

object Document:
  given encoder: JsonEncoder[Document] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[Document] = DeriveJsonDecoder.gen
