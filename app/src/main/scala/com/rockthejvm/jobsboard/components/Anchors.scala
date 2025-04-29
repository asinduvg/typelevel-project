package com.rockthejvm.jobsboard.components

import tyrian.*
import tyrian.Html.*
import com.rockthejvm.jobsboard.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.pages.*

object Anchors {
  def renderSimpleNavLink(text: String, location: String, cssClass: String = "") =
    renderNavLink(text, location, cssClass)(Router.ChangeLocation(_))

  def renderNavLink(text: String, location: String, cssClass: String = "")(location2Msg: String => App.Msg) =
    li(`class` := "nav-item")(
      a(
        href := location,
        `class` := cssClass,
        onEvent(
          "click",
          e => {
            e.preventDefault()
            location2Msg(location)
          }
        )
      )(text)
    )
}