package com.github.zavalit

import java.net.URI

object Domain {

  sealed trait Business

  case class Coordinates(x: Int, y: Int) extends Business

  case class Time(value: Long) extends Business

  case class Line(id: String) extends Business

  case class Stop(id: Int)

  sealed trait Client

  sealed trait Ref extends Client

  object WebsocketRef {
    import server.Websocket.Config
    def create(config: Config, coordinates: Coordinates, time: Time): WebsocketRef =
      WebsocketRef(new URI(s"ws://${config.host}:${config.port}"), s"${coordinates.x}x${coordinates.y}_${time.value}")

  }

  case class WebsocketRef(host: URI, ref: String) extends Ref {
    def hostRef: WebsocketHostRef = WebsocketHostRef(s"${host.toString}/${ref}")
  }

  case class WebsocketHostRef(ref: String) extends Ref
}
