package com.rockthejvm.jobsboard.components

import tyrian.*
import tyrian.Html.*
import com.rockthejvm.jobsboard.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.pages.*

object Anchors {
  def renderSimpleNavLink(text: String, location: String) =
    renderNavLink(text, location)(Router.ChangeLocation(_))

  def renderNavLink(text: String, location: String)(location2Msg: String => App.Msg) =
    li(`class` := "nav-item")(
      a(
        href := location,
        `class` := "nav-link",
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