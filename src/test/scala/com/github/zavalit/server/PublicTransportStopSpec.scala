package com.github.zavalit.server

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import com.github.zavalit.Domain._
import com.github.zavalit.PublicTransportStop
import com.github.zavalit.PublicTransportStop.{ PublicTransportStopReply, TrackLocationLines, TrackingExposed }
import org.scalatest._

class PublicTransportStopSpec extends WordSpec with BeforeAndAfterAll with Matchers {
  val testKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  "PublicTransportStop actor" should {
    "give a reference for query back" in {

      val pinger = testKit.spawn(PublicTransportStop(Websocket.Config("localhost", 1234)))
      val probe = testKit.createTestProbe[PublicTransportStopReply]()
      pinger ! TrackLocationLines(Coordinates(1, 2), Time(3L), probe.ref)

      probe.expectMessage(TrackingExposed(WebsocketHostRef("ws://localhost:1234/1x2_3")))
    }
  }

}
