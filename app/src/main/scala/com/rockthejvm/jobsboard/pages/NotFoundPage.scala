package com.rockthejvm.jobsboard.pages

import tyrian.*
import tyrian.Html.*
import cats.effect.IO
import com.rockthejvm.jobsboard.*
import com.rockthejvm.jobsboard.common.Constants
final case class NotFoundPage() extends Page {

  override def view: Html[App.Msg] =
    div(`class` := "row")(
      div(`class` := "col-md-5 p-0")(
        div(`class` := "logo")(
          img(src   := Constants.logoImage, alt := "")
        )
      ),
      div(`class` := "col-md-7")(
        div(`class` := "form-section")(
          div(`class` := "top-section")(
            h1(span("\uD83E\uDD26\u200D♂\uFE0F Ouch!")),
            div("This page doesn't exist. You lost or something?")
          )
        )
      )
    )

  override def initCmd: Cmd[IO, App.Msg] =
    Cmd.None

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) =
    (this, Cmd.None)

}
