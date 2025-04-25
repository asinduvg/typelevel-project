package com.rockthejvm.jobsboard.components

import tyrian.*
import tyrian.Html.*
import scala.scalajs.js
import scala.scalajs.js.annotation.*
import com.rockthejvm.jobsboard.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.pages.*
import com.rockthejvm.jobsboard.components.Anchors

object Header {

  // public API
  def view =
    div(`class` := "header-container")(
      renderLogo(),
      div(`class` := "header-nav")(
        ul(`class` := "header-links")(
          renderNavLinks()
        )
      )
    )

  // private API

  @js.native
  @JSImport("/static/img/fiery-lava 128x128.png", JSImport.Default)
  private val logoImage: String = js.native

  private def renderLogo() =
    a(
      href := "/",
      onEvent(
        "click",
        e => {
          e.preventDefault()
          Router.ChangeLocation("/")
        }
      )
    )(
      img(
        `class` := "home-logo",
        src     := logoImage,
        alt     := "Rock the JVM"
      )
    )

  private def renderNavLinks(): List[Html[App.Msg]] = {
    val constantsLinks = List(
      Anchors.renderSimpleNavLink("Jobs", Page.Urls.JOBS),
      Anchors.renderSimpleNavLink("Post Job", Page.Urls.POST_JOB)
    )

    val unauthedLinks = List(
      Anchors.renderSimpleNavLink("Login", Page.Urls.LOGIN),
      Anchors.renderSimpleNavLink("Signup", Page.Urls.SIGNUP)
    )

    val authedLinks = List(
      Anchors.renderSimpleNavLink("Profile", Page.Urls.PROFILE),
      Anchors.renderNavLink("Log Out", Page.Urls.LOGOUT)(_ => Session.Logout)
    )

    constantsLinks ++ {
      if (Session.isActive) authedLinks
      else unauthedLinks
    }

  }
}
