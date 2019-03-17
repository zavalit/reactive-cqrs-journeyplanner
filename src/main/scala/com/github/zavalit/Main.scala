package com.github.zavalit

import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, Terminated }
import akka.actor.{ Cancellable, CoordinatedShutdown, Scheduler, ActorSystem => UntypedSystem }
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import com.github.zavalit.server.{ Websocket, Http }
import com.typesafe.scalalogging.LazyLogging
import com.github.zavalit.{ PublicTransportStream => PTStream, PublicTransportStreamFixture => Fixture }
import pureconfig.loadConfigOrThrow
import pureconfig.generic.auto._

object Main extends LazyLogging {

  import akka.actor.typed.scaladsl.adapter._

  sealed trait Command

  final object TopLevelActorTerminated extends Reason

  final case class Config(ws: Websocket.Config, http: Http.Config)

  def main(args: Array[String]): Unit = {

    val config = loadConfigOrThrow[Config]("journeyplanner")
    ActorSystem(Main(config), "journeyplanner")

  }

  def apply(config: Config): Behavior[Command] =
    Behaviors.setup { context =>

      import context.executionContext
      implicit val untypedSystem: UntypedSystem = context.system.toUntyped
      implicit val mat: Materializer = ActorMaterializer()(context.system)
      implicit val scheduler: Scheduler = untypedSystem.scheduler

      val publicTransportStopActor = context.spawn(PublicTransportStop(config.ws), "public-transport-stop")

      context.watch(publicTransportStopActor)

      // generate line's arrivement stream
      PTStream.graph[Cancellable](
        Fixture.generateRandomTrackingSource,
        PTStream.arraivingLineTrackingSink(publicTransportStopActor))
        .run()

      // start server
      Http(config.http, publicTransportStopActor)
      Websocket(config.ws)

      // listen for crash
      Behaviors.receiveSignal {
        case (_, Terminated(actor)) =>
          logger.error(s"$actor was terminated! Shutting down the system")
          CoordinatedShutdown(context.system.toUntyped).run(TopLevelActorTerminated)
          Behaviors.stopped
      }

    }

}