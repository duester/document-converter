package ru.duester.converter.error

/** Possible errors while converting documents
  */
enum ConversionError:
  /** Node with required type is missing
    *
    * @param nodeType
    *   required node type
    */
  case MissingNodeError(nodeType: String)

  /** Child node with required type is missing
    *
    * @param nodeType
    *   required node type
    * @param childNodeType
    *   required child node type
    */
  case MissingChildError(nodeType: String, childNodeType: String)

  /** Node with required attribute is missing
    *
    * @param nodeType
    *   required node type
    * @param attribute
    *   required attribute name
    */
  case MissingAttributeError(nodeType: String, attribute: String)

  /** Node has attribute with wrong value
    *
    * @param nodeType
    *   node type
    * @param attribute
    *   attribute name
    * @param value
    *   wrong attribute value
    */
  case FormatError(nodeType: String, attribute: String, value: String)

  /** Other error
    *
    * @param nodeType
    *   node type
    * @param message
    *   error message
    */
  case OtherError(nodeType: String, message: String)
