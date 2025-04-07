package com.rockthejvm.jobsboard.pages

import io.circe.parser.*
import io.circe.generic.auto.*
import cats.effect.IO
import com.rockthejvm.jobsboard.App
import com.rockthejvm.jobsboard.common.{Constants, Endpoint}
import com.rockthejvm.jobsboard.core.Session
import com.rockthejvm.jobsboard.domain.auth.NewPasswordInfo
import tyrian.Html.*
import tyrian.http.{HttpError, Method, Response, Status}
import tyrian.{Cmd, Html}

final case class ProfilePage(
    oldPassword: String = "",
    password: String = "",
    status: Option[Page.Status] = None
) extends FormPage("Profile", status) {
  import ProfilePage.*
  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case UpdateOldPassword(op) =>
      (this.copy(oldPassword = op), Cmd.None)
    case UpdateNewPassword(np) =>
      (this.copy(password = np), Cmd.None)
    case AttemptChangePassword =>
      (this, Commands.resetPassword(oldPassword, password))
    case ChangePasswordError(error) =>
      (setErrorStatus(error), Cmd.None)
    case ChangePasswordSuccess =>
      (setSuccessStatus("Success! Your password has been changed"), Cmd.None)
    case _ => (this, Cmd.None)
  }

  override def view: Html[App.Msg] =
    if (Session.isActive) super.view
    else renderInvalidPage

  override protected def renderFormContent(): List[Html[App.Msg]] = List(
    renderInput("Old Password", "oldPassword", "password", true, UpdateOldPassword(_)),
    renderInput("New Password", "password", "password", true, UpdateNewPassword(_)),
    button(`type` := "button", onClick(AttemptChangePassword))("Change Password")
  )

  private def renderInvalidPage =
    div(
      h1("Profile"),
      div("Ouch! It seems you are not logged in yet.")
    )

  // util
  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))
}

private object ProfilePage {
  sealed trait Msg                                          extends App.Msg
  private case class UpdateOldPassword(oldPassword: String) extends Msg
  private case class UpdateNewPassword(password: String)    extends Msg
  private case object AttemptChangePassword                 extends Msg
  private case class ChangePasswordError(str: String)       extends Msg
  private case object ChangePasswordSuccess                 extends Msg

  object Endpoints {
    val resetPassword: Endpoint[Msg] = new Endpoint[Msg] {

      override val location: String          = Constants.endpoints.changePassword
      override val method: Method            = Method.Put
      override val onError: HttpError => Msg = e => ChangePasswordError(e.toString)
      override val onResponse: Response => Msg = _.status match {
        case Status(200, _) => ChangePasswordSuccess
        case Status(404, _) => ChangePasswordError("Funny. Server says this user doesn't exist.")
        case Status(s, _) if s >= 400 && s < 500 => ChangePasswordError("Invalid credentials.")
        case _ => ChangePasswordError("Unknown reply from server. Something's wrong!")
      }
    }
  }

  object Commands {
    def resetPassword(oldPassword: String, password: String): Cmd[IO, Msg] =
      Endpoints.resetPassword.callAuthorized(NewPasswordInfo(oldPassword, password))
  }

}
