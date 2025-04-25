package com.rockthejvm.jobsboard.pages

import cats.effect.IO
import com.rockthejvm.jobsboard.App
import tyrian.*
import tyrian.Html.*
import com.rockthejvm.jobsboard.*
import com.rockthejvm.jobsboard.core.Router
import org.scalajs.dom.{File, HTMLFormElement, document}
import tyrian.Tyrian.HTMLInputElement

import scala.concurrent.duration.FiniteDuration

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
        name    := "signin",
        `class` := "form",
        id      := "form",
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
      label(`for` := uid, `class` := "form-label")(
        if (isRequired) span("*") else span(),
        text(name)
      ),
      input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
    )

  protected def renderImageUploadInput(
      name: String,
      uid: String,
      imgSrc: Option[String],
      onChange: Option[File] => App.Msg
  ) =
    div(`class` := "form-input")(
      label(`for` := uid, `class` := "form-label")(name),
      input(`type` := "file", `class` := "form-control", id := uid, accept := "image/*", onEvent("change", e => {
        val imageInput = e.target.asInstanceOf[HTMLInputElement]
        val fileList = imageInput.files // FileList
        if (fileList.length > 0)
          onChange(Some(fileList(0)))
        else
          onChange(None)
      })),
      img(
        id := "preview",
        src := imgSrc.getOrElse(""),
        alt := "preview",
        width := "100",
        height := "100"
      )
    )
    
  protected def renderTextArea(name: String, uid: String, isRequired: Boolean, onChange: String => App.Msg) =
    div(`class` := "form-input")(
      label(`for` := name, `class` := "form-label")(
        if (isRequired) span("*") else span(),
        text(name)
      ),
      textarea(`class` := "form-control", id := uid, onInput(onChange))("")
    )

  override def initCmd: Cmd[IO, App.Msg] = clearForm()

  override def view: Html[App.Msg] = renderForm()

  /*
    check if the form has loaded (if it's present on the page)
      document.getElementById()
    check again, while the element is null, with a space of 100 millis

    use IO effects!
   */
  private def clearForm() =
    Cmd.Run[IO, Unit, App.Msg] {
      def effect: IO[Option[HTMLFormElement]] = for {
        maybeForm <- IO(Option(document.getElementById("form").asInstanceOf[HTMLFormElement]))
        finalForm <-
          if (maybeForm.isEmpty) IO.sleep(FiniteDuration(100, "millis")) *> effect
          else IO(maybeForm)
      } yield finalForm

      effect.map(_.foreach(_.reset()))

    }(_ => App.NoOp)
}
