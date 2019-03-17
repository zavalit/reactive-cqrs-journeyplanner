package com.github.zavalit.server

import akka.NotUsed
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.stream.{ Materializer, OverflowStrategy }
import akka.stream.scaladsl.SourceQueueWithComplete
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.github.zavalit.Domain._
import akka.actor.{ ActorSystem => UntypedActorSystem }
import akka.http.scaladsl.{ Http => AkkaHttp }

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives._
import com.github.zavalit.PublicTransportStop.ArraivingLine
import com.typesafe.scalalogging.LazyLogging

import scala.util.{ Failure, Success }
import io.circe.generic.auto._
import io.circe.syntax._
import scala.collection.mutable.{ Map => MMap }
object TrackingSubscriber {

  private val trackQueuesMap: MMap[String, List[TextMessage => Any]] = MMap()

  def flow(ref: String): Flow[Message, Message, NotUsed] = {
    val inbound: Sink[Message, Any] = Sink.ignore
    val outboud: Source[Message, SourceQueueWithComplete[Message]] = Source.queue[Message](16, OverflowStrategy.fail)

    Flow.fromSinkAndSourceMat(inbound, outboud)((_, outboudMat) => {
      trackQueuesMap(ref) = outboudMat.offer _ :: trackQueuesMap.getOrElse(ref, List())
      NotUsed
    })
  }

  def track(msg: ArraivingLine, ref: WebsocketRef) = {
    for (queue <- trackQueuesMap.getOrElse(ref.ref, List())) queue(TextMessage.Strict(msg.asJson.toString()))
  }

}

object Websocket extends LazyLogging {

  import TrackingSubscriber._

  final case class Config(host: String, port: Int)

  def apply(config: Config)(implicit untypedSystem: UntypedActorSystem, materializer: Materializer, executionContext: ExecutionContext): Unit = {
    val route =
      path(Segment) { ref =>
        logger.info(s"client connected for ${ref}")
        handleWebSocketMessages(flow(ref))
      }

    val bindingFuture =
      AkkaHttp().bindAndHandle(route, interface = config.host, port = config.port)

    import untypedSystem.dispatcher // for the future transformations
    bindingFuture.onComplete {
      case Success(_) => logger.info(s"websocket exposed at ws://${config.host}:${config.port}/!")
      case Failure(error) => logger.error(s"Failed: ${error.getMessage}")
    }
  }

}
