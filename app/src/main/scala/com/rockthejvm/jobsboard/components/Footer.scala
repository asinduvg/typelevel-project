package com.rockthejvm.jobsboard.components

import com.rockthejvm.jobsboard.*
import tyrian.*
import tyrian.Html.*

import scala.scalajs.js.Date

object Footer {
  def view: Html[App.Msg] =
    div(`class` := "footer")(
      p(
        text("Written in "),
        a(href := "https://scala-lang.org", target := "blank")("Scala"),
        text(" with ❤\uFE0F at "),
        a(href := "https://rockthejvm.com", target := "blank")("Rock the JVM")
      ),
      p(s"© Rock the JVM ${new Date().getFullYear()}, don't copy me 😘")
    )
}
