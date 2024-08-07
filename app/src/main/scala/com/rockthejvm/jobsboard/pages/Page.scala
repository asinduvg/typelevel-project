package com.rockthejvm.jobsboard.pages

import tyrian.Cmd
import cats.effect.IO
import tyrian.Html

import com.rockthejvm.jobsboard.* 

object Page {
  trait Msg

  enum StatusKind {
    case SUCCESS, ERROR, LOADING
  }
  final case class Status(message: String, kind: StatusKind)

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
  def initCmd: Cmd[IO, App.Msg]
  // update
  def update(msg: App.Msg): (Page, Cmd[IO, App.Msg])
  // render
  def view: Html[App.Msg]
}

// login page
// signup page
// forgot password page
// recover password page
// job list page == home page
// individual job page
// not found page
