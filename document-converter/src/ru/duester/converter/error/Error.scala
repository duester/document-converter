package ru.duester.converter.error

enum ConversionError:
  case MissingNode(nodeType: String)
  case MissingTextContent(nodeType: String)
  case MissingBinaryContent(nodeType: String)
  case MissingChild(nodeType: String, childNodeType: String)
  case MissingAttributeError(nodeType: String, attribute: String)
  case FormatError(nodeType: String, value: String)
  case OtherError(nodeType: String, message: String)
