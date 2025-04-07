package com.rockthejvm.jobsboard.pages

import cats.effect.IO
import com.rockthejvm.jobsboard.App
import tyrian.*
import tyrian.Html.*
import com.rockthejvm.jobsboard.*
import com.rockthejvm.jobsboard.core.Router

abstract class FormPage(title: String, status: Option[Page.Status]) extends Page {
  // abstract API
  protected def renderFormContent(): List[Html[App.Msg]] // for every page to override

  // protected API
  protected def renderForm(): Html[App.Msg] =
    div(`class` := "form-section")(
      // title: Log In
      div(`class` := "top-section")(
        h1(title)
      ),
      // form
      form(
        name := "signin",
        `class` := "form",
        onEvent(
          "submit",
          e => {
            e.preventDefault()
            App.NoOp
          }
        )
      )(
        renderFormContent()
      ),
      status.map(s => div(s.message)).getOrElse(div())
    )

  protected def renderInput(
                           name: String,
                           uid: String,
                           kind: String,
                           isRequired: Boolean,
                           onChange: String => App.Msg
                         ) =
    div(`class` := "form-input")(
      label(`for` := name, `class` := "form-label")(
        if (isRequired) span("*") else span(),
        text(name)
      ),
      input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
    )

  protected def renderAuxLink(location: String, text: String): Html[App.Msg] =
    a(
      href := location,
      `class` := "aux-link",
      onEvent(
        "click",
        e => {
          e.preventDefault()
          Router.ChangeLocation(location)
        }
      )
    )(text)
  
  override def initCmd: Cmd[IO, App.Msg] = Cmd.None

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = ???

  override def view: Html[App.Msg] = renderForm()
}
