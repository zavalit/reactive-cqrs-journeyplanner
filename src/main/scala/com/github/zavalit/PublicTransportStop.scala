package com.github.zavalit

import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.scalalogging.LazyLogging
import akka.actor.typed.{ ActorRef, Behavior }
import com.github.zavalit.server.{ TrackingSubscriber }
import server.Websocket.{ Config => WSConfig }

object PublicTransportStop extends LazyLogging {

  import Domain._

  sealed trait Command

  final case class TrackLocationLines(
    coordinates: Coordinates,
    time: Time,
    replyTo: ActorRef[PublicTransportStopReply]) extends Command

  final case class ArraivingLine(
    line: Line,
    stop: Stop,
    scheduled_time: Time,
    expected_time: Time) extends Command

  final case object Complete extends Command

  final case class Fail(ex: Throwable) extends Command

  sealed trait PublicTransportStopReply

  final case object CoordinatesUnknown extends PublicTransportStopReply

  final case class TrackingExposed(ref: WebsocketHostRef) extends PublicTransportStopReply

  def apply(ws_config: WSConfig, tracked_locations: List[WebsocketRef] = List()): Behavior[Command] = {
    Behaviors.receiveMessage {

      case TrackLocationLines(coordinates, time, replyTo) =>
        val ref = WebsocketRef.create(ws_config, coordinates, time)

        replyTo ! TrackingExposed(ref.hostRef)

        if (!tracked_locations.contains(ref)) PublicTransportStop(ws_config, ref :: tracked_locations)
        else Behavior.same

      case a: ArraivingLine =>
        for (ref <- tracked_locations) TrackingSubscriber.track(a, ref)
        Behavior.same

      case Complete =>
        logger.info("complete actor PublicTransportStop")
        Behavior.stopped

      case Fail(ex) =>
        logger.error(s"actor failed: ${ex}")
        Behavior.stopped

    }
  }

  def trackLocationLines(coordinates: Coordinates, time: Long)(replyTo: ActorRef[PublicTransportStopReply]): TrackLocationLines =
    TrackLocationLines(coordinates, Time(time), replyTo)

}
