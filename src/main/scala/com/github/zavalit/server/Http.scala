package com.github.zavalit.server

import akka.actor.CoordinatedShutdown.{ PhaseServiceRequestsDone, PhaseServiceUnbind, Reason }
import akka.http.scaladsl.{ Http => AkkaHttp }
import akka.actor.typed.ActorRef
import akka.Done
import scala.concurrent.duration._
import scala.util.{ Failure, Success }
import akka.actor.{ CoordinatedShutdown, Scheduler, ActorSystem => UntapedSystem }
import akka.stream.Materializer
import com.github.zavalit.PublicTransportStop
import com.github.zavalit.PublicTransportStop.PublicTransportStopReply
import com.typesafe.scalalogging.LazyLogging
import akka.http.scaladsl.model.StatusCodes.{ BadRequest, Conflict, Created, NoContent, OK, Unauthorized }
import akka.http.scaladsl.server.{ Directives, Route }
import com.github.zavalit.Domain.Coordinates
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import akka.pattern.after
import scala.concurrent.Future

object Http extends LazyLogging {

  final case class TrackLocation(coordinates: Coordinates, timestamp: Long)

  final object BindFailure extends Reason

  def apply(publicTransportStop: ActorRef[PublicTransportStop.Command])(implicit untypedSystem: UntapedSystem, mat: Materializer): Unit = {
    import untypedSystem.dispatcher

    val (address, port) = ("0.0.0.0", 8081)
    val shutdown = CoordinatedShutdown(untypedSystem)

    AkkaHttp()
      .bindAndHandle(route(publicTransportStop), address, port)
      .onComplete {
        case Failure(cause) =>
          logger.error(s"Shutting down, because cannot bind to $address:$port!", cause)
          CoordinatedShutdown(untypedSystem).run(BindFailure)

        case Success(binding) =>
          logger.info(s"Listening for HTTP connections on ${binding.localAddress}")
          shutdown.addTask(PhaseServiceUnbind, "api.unbind") { () =>
            binding.unbind()
          }
          shutdown.addTask(PhaseServiceRequestsDone, "api.requests-done") { () =>
            after(30 seconds, untypedSystem.scheduler)(Future.successful(Done))
          }
      }

  }

  def route(publicTransportStop: ActorRef[PublicTransportStop.Command])(implicit untypedSystem: UntapedSystem) = {
    import Directives._
    implicit val scheduler: Scheduler = untypedSystem.scheduler
    implicit val timeout: Timeout = 5 seconds

    import com.github.zavalit.helper.CirceSupport._
    import io.circe.generic.auto._

    pathPrefix("tracklines") {
      import PublicTransportStop._
      pathEnd {
        post {
          entity(as[TrackLocation]) {
            case t: TrackLocation =>
              onSuccess(publicTransportStop ? trackLocationLines(t.coordinates, t.timestamp)) {
                case t: TrackingExposed => complete(t.ref)
                case CoordinatesUnknown => complete(BadRequest)
              }
          }
        }
      }
    }
  }

}
