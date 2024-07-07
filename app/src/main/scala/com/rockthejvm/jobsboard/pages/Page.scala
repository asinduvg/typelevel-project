package com.rockthejvm.jobsboard.pages

import tyrian.Cmd
import cats.effect.IO
import tyrian.Html

object Page {
  trait Msg

  object Urls {
    val LOGIN            = "/login"
    val SIGNUP           = "/signup"
    val FORGOT_PASSWORD  = "/forgotpassword"
    val RECOVER_PASSWORD = "/recoverpassword"
    val JOBS             = "/jobs"
    val EMPTY            = ""
    val HOME             = "/"
  }

  import Urls.*
  def get(location: String) = location match {
    case `LOGIN`                   => LoginPage()
    case `SIGNUP`                  => SignUpPage()
    case `FORGOT_PASSWORD`         => ForgotPasswordPage()
    case `RECOVER_PASSWORD`        => RecoverPasswordPage()
    case `JOBS` | `EMPTY` | `HOME` => JobListPage()
    case s"/jobs/$id"              => JobPage(id)
    case _                         => NotFoundPage()
  }
}

abstract class Page {
  // API
  // send a command upon instantiating
  def initCmd: Cmd[IO, Page.Msg]
  // update
  def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg])
  // render
  def view: Html[Page.Msg]
}

// login page
// signup page
// forgot password page
// recover password page
// job list page == home page
// individual job page
// not found page