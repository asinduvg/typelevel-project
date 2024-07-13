package com.rockthejvm.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import cats.effect.IO

import com.rockthejvm.jobsboard.* 
final case class NotFoundPage() extends Page {

  override def view: Html[App.Msg] =
    div("NotFoundPage")

  override def initCmd: Cmd[IO, App.Msg] =
    Cmd.None

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) =
    (this, Cmd.None)

}
