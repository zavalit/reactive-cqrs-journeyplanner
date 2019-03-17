package com.github.zavalit

import akka.NotUsed
import akka.actor.Cancellable
import akka.actor.typed.ActorRef
import akka.stream.ClosedShape
import akka.stream.scaladsl.{ Broadcast, GraphDSL, RunnableGraph, Sink, Source }
import akka.stream.typed.scaladsl.ActorSink
import com.github.zavalit.Domain._
import com.github.zavalit.PublicTransportStop.ArraivingLine
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.util.Random

object PublicTransportStream extends LazyLogging {

  def graph[Mat](in: Source[PublicTransportStop.Command, Mat], trackingSink: Sink[PublicTransportStop.Command, NotUsed]): RunnableGraph[NotUsed] =
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>

      import GraphDSL.Implicits._

      val bcast = builder.add(Broadcast[PublicTransportStop.Command](2))

      val loggingSink = Sink.foreach { line: PublicTransportStop.Command => logger.info(line.toString) }

      in ~>
        bcast ~> trackingSink
      bcast ~> loggingSink

      ClosedShape
    })

  def arraivingLineTrackingSink(actor: ActorRef[PublicTransportStop.Command]): Sink[PublicTransportStop.Command, NotUsed] =
    ActorSink.actorRef[PublicTransportStop.Command](actor, PublicTransportStop.Complete, PublicTransportStop.Fail.apply)

}

object PublicTransportStreamFixture {

  val lines: Seq[Line] = Seq(Line("M4"), Line("200"), Line("S75"))

  val random = new Random()

  def generateRandomTrackingSource: Source[PublicTransportStop.Command, Cancellable] = Source
    .tick(0 second, 1 second, 1)
    .map { _ =>
      val now = System.currentTimeMillis()
      val scheduleTime = now + 2 * 1000L * 60
      val expectedTime = now - random.nextInt(8) * 1000L * 60
      val line = lines(random.nextInt(2))

      ArraivingLine(line, Stop(1), Time(scheduleTime), Time(expectedTime))
    }

}
