package com.rockthejvm.jobsboard

import scala.scalajs.js.annotation.*
import org.scalajs.dom.{document, console}
import tyrian.*
import tyrian.Html.*
import tyrian.cmds.Logger
import cats.effect.IO
import scala.concurrent.duration.*

object PlaygroundApp {
  sealed trait Msg
  case class Increment(amount: Int) extends Msg
  case class Model(count: Int)
}

class PlaygroundApp extends TyrianApp[PlaygroundApp.Msg, PlaygroundApp.Model] {
  //                      ^^message ^^ model=state
  /*
    We can send messages by
        - trigger a command
        - create a subscription
        - listening for an event
   */
  import PlaygroundApp.*

  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(0), Cmd.None)

  // view triggered whenever model changes
  override def view(model: Model): Html[Msg] =
    div(
      button(onClick(Increment(1)))("Increase"),
      button(onClick(Increment(-1)))("Decrease"),
      div(s"Tyrian running: ${model.count}")
    )

  // potentially endless stream of messages
  override def subscriptions(model: Model): Sub[IO, Msg] =
    // Sub.None
    Sub.every[IO](1.second).map(_ => Increment(1))

    // model can change by receiving messages
    // model => message => (new model, _____)
    // update triggered whenever we get a new message
  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = { case Increment(amount) =>
    // console.log(s"Changing count by $amount")
    (model.copy(count = model.count + amount), Logger.consoleLog[IO](s"Changing count by $amount"))
  }

}
