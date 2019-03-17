package com.github.zavalit.helper

import akka.http.scaladsl.marshalling.{ Marshaller, ToEntityMarshaller }
import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, Unmarshaller }
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.{ ContentType, ContentTypeRange, HttpEntity, MediaType }
import akka.util.ByteString
import cats.data.NonEmptyList
import io.circe._
import cats.syntax.show.toShow

/**
 * Automatic to and from JSON marshalling/unmarshalling using an in-scope circe protocol.
 */

object CirceSupport extends BaseCirceSupport

trait BaseCirceSupport {

  final case class DecodingFailures(failures: NonEmptyList[DecodingFailure]) extends Exception {
    override def getMessage = failures.toList.map(_.show).mkString("\n")
  }

  def unmarshallerContentTypes: Seq[ContentTypeRange] =
    mediaTypes.map(ContentTypeRange.apply)

  def mediaTypes: Seq[MediaType.WithFixedCharset] =
    List(`application/json`)

  /**
   * `Json` => HTTP entity
   *
   * @return marshaller for JSON value
   */
  implicit final def jsonMarshaller(implicit printer: Printer = Printer.noSpaces): ToEntityMarshaller[Json] =
    Marshaller.oneOf(mediaTypes: _*) { mediaType =>
      Marshaller.withFixedContentType(ContentType(mediaType)) { json =>
        HttpEntity(
          mediaType,
          ByteString(printer.prettyByteBuffer(json, mediaType.charset.nioCharset())))
      }
    }

  /**
   * `A` => HTTP entity
   *
   * @tparam A type to encode
   * @return marshaller for any `A` value
   */
  implicit final def marshaller[A: Encoder](implicit printer: Printer = Printer.noSpaces): ToEntityMarshaller[A] =
    jsonMarshaller(printer).compose(Encoder[A].apply)

  /**
   * HTTP entity => `Json`
   *
   * @return unmarshaller for `Json`
   */
  implicit final val jsonUnmarshaller: FromEntityUnmarshaller[Json] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(unmarshallerContentTypes: _*)
      .map {
        case ByteString.empty => throw Unmarshaller.NoContentException
        case data => jawn.parseByteBuffer(data.asByteBuffer).fold(throw _, identity)
      }

  /**
   * HTTP entity => `A`
   *
   * @tparam A type to decode
   * @return unmarshaller for `A`
   */
  implicit def unmarshaller[A: Decoder]: FromEntityUnmarshaller[A] = {
    def decode(json: Json) =
      Decoder[A]
        .accumulating(json.hcursor)
        .fold(failures => throw DecodingFailures(failures), identity)
    jsonUnmarshaller.map(decode)
  }
}
