package com.rockthejvm.jobsboard.components

import tyrian.*
import tyrian.Html.*
import scala.scalajs.js
import scala.scalajs.js.annotation.*
import com.rockthejvm.jobsboard.*
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.pages.*

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
      renderSimpleNavLink("Jobs", Page.Urls.JOBS),
      renderSimpleNavLink("Post Job", Page.Urls.POST_JOB)
    )

    val unauthedLinks = List(
      renderSimpleNavLink("Login", Page.Urls.LOGIN),
      renderSimpleNavLink("Signup", Page.Urls.SIGNUP)
    )

    val authedLinks = List(
      renderSimpleNavLink("Profile", Page.Urls.PROFILE),
      renderNavLink("Log Out", Page.Urls.LOGOUT)(_ => Session.Logout)
    )

    constantsLinks ++ {
      if (Session.isActive) authedLinks
      else unauthedLinks
    }

  }

  private def renderSimpleNavLink(text: String, location: String) =
    renderNavLink(text, location)(Router.ChangeLocation(_))

  private def renderNavLink(text: String, location: String)(location2Msg: String => App.Msg) =
    li(`class` := "nav-item")(
      a(
        href    := location,
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
