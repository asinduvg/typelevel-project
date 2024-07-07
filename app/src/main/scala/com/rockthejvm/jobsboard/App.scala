package com.rockthejvm.jobsboard

import scala.scalajs.js.annotation.*
import org.scalajs.dom.{window}
import tyrian.*
import tyrian.Html.*
import tyrian.cmds.Logger
import cats.effect.IO
import scala.concurrent.duration.*

import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.components.*
import com.rockthejvm.jobsboard.pages.*

object App {
  type Msg = Router.Msg | Page.Msg
  case class Model(router: Router, page: Page)
}

@JSExportTopLevel("RockTheJvmApp")
class App extends TyrianApp[App.Msg, App.Model] {
  //                      ^^message ^^ model=state
  /*
    We can send messages by
        - trigger a command
        - create a subscription
        - listening for an event
   */
  import App.*

  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    val location            = window.location.pathname
    val page                = Page.get(location)
    val pageCmd             = page.initCmd
    val (router, routerCmd) = Router.startAt(location)
    (Model(router, page), routerCmd |+| pageCmd)

  // view triggered whenever model changes
  override def view(model: Model): Html[Msg] =
    div(
      Header.view,
      model.page.view
    )

  // potentially endless stream of messages
  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.make( // listener for browser history changes
      "urlChange",
      model.router.history.state.discrete
        .map(_.get)
        .map(newLocation => Router.ChangeLocation(newLocation, true))
    )

    // model can change by receiving messages
    // model => message => (new model, _____)
    // update triggered whenever we get a new message
  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case msg: Router.Msg =>
      val (newRouter, routerCmd) = model.router.update(msg)
      if (model.router == newRouter) // no change necessary
        (model, Cmd.None)
      else
        // location changed, need to re-render the appropriate page
        val newPage    = Page.get(newRouter.location)
        val newPageCmd = newPage.initCmd
        (model.copy(router = newRouter, page = newPage), routerCmd |+| newPageCmd)
    case msg: Page.Msg =>
      val (newPage, cmd) = model.page.update(msg)
      (model.copy(page = newPage), cmd)
  }

}
