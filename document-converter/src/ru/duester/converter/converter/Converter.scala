package ru.duester.converter.converter

import ru.duester.converter.error.ConversionError
import ru.duester.converter.model.Document
import zio.IO

trait FormatConverter[T]:
  def fromFormat(document: T): IO[ConversionError, Document]
  def toFormat(document: Document): IO[ConversionError, T]

class DocumentConverter[T: FormatConverter, U: FormatConverter]:
  def convert(document: T): IO[ConversionError, U] =
    val converterT = summon[FormatConverter[T]]
    val converterU = summon[FormatConverter[U]]
    for {
      intermediateDocument <- converterT.fromFormat(document)
      finalDocument <- converterU.toFormat(intermediateDocument)
    } yield finalDocument
