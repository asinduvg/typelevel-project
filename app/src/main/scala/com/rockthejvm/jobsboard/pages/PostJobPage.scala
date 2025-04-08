package com.rockthejvm.jobsboard.pages

import io.circe.parser.*
import io.circe.generic.auto.*
import cats.effect.IO
import com.rockthejvm.jobsboard.App
import com.rockthejvm.jobsboard.common.{Constants, Endpoint}
import com.rockthejvm.jobsboard.core.Session
import com.rockthejvm.jobsboard.domain.job.JobInfo
import tyrian.*
import tyrian.Html.*
import tyrian.cmds.Logger
import tyrian.http.*

case class PostJobPage(
    company: String = "",
    title: String = "",
    description: String = "",
    externalUrl: String = "",
    location: String = "",
    remote: Boolean = false,
    salaryLo: Option[Int] = None,
    salaryHi: Option[Int] = None,
    currency: Option[String] = None,
    country: Option[String] = None,
    tags: Option[String] = None, // TODO: parse the tags before sending them to the server
    image: Option[String] = None,
    seniority: Option[String] = None,
    other: Option[String] = None,
    status: Option[Page.Status] = None
) extends FormPage("Post Job", status) {
  import PostJobPage.*

  override def view: Html[App.Msg] =
    if (Session.isActive) super.view
    else renderInvalidPage

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case UpdateCompany(v)     => (this.copy(company = v), Cmd.None)
    case UpdateTitle(v)       => (this.copy(title = v), Cmd.None)
    case UpdateDescription(v) => (this.copy(description = v), Cmd.None)
    case UpdateExternalUrl(v) => (this.copy(externalUrl = v), Cmd.None)
    case ToggleRemote         => (this.copy(remote = !this.remote), Cmd.None)
    case UpdateLocation(v)    => (this.copy(location = v), Cmd.None)
    case UpdateSalaryLo(v)    => (this.copy(salaryLo = Some(v)), Cmd.None)
    case UpdateSalaryHi(v)    => (this.copy(salaryHi = Some(v)), Cmd.None)
    case UpdateCurrency(v)    => (this.copy(currency = Some(v)), Cmd.None)
    case UpdateCountry(v)     => (this.copy(country = Some(v)), Cmd.None)
    case UpdateTags(v)        => (this.copy(tags = Some(v)), Cmd.None)
    case UpdateSeniority(v)   => (this.copy(seniority = Some(v)), Cmd.None)
    case UpdateOther(v)       => (this.copy(other = Some(v)), Cmd.None)
    case AttemptPostJob =>
      (
        this,
        Commands.postJob(
          company,
          title,
          description,
          externalUrl,
          location,
          remote,
          salaryLo,
          salaryHi,
          currency,
          country,
          tags,
          image,
          seniority,
          other
        )
      )
    case PostJobError(error) =>
      (setErrorStatus(error), Cmd.None)
    case PostJobSuccess(jobId) =>
      (setSuccessStatus("Success!"), Logger.consoleLog[IO](s"Posted job with id $jobId"))
    case _ => (this, Cmd.None)
  }

  override protected def renderFormContent(): List[Html[App.Msg]] = List(
    renderInput("Company", "company", "text", true, UpdateCompany(_)),
    renderInput("Title", "title", "text", true, UpdateTitle(_)),
    renderTextArea("Description", "description", true, UpdateDescription(_)),
    renderInput("ExternalUrl", "externalUrl", "text", true, UpdateExternalUrl(_)),
    renderInput("Remote", "remote", "checkbox", true, _ => ToggleRemote),
    renderInput("Location", "location", "text", true, UpdateLocation(_)),
    renderInput("SalaryLo", "salaryLo", "number", true, value => UpdateSalaryLo(value.toInt)),
    renderInput("SalaryHi", "salaryHi", "number", false, value => UpdateSalaryHi(value.toInt)),
    renderInput("Currency", "currency", "text", false, UpdateCurrency(_)),
    renderInput("Country", "country", "text", false, UpdateCountry(_)),
    renderInput("Tags", "tags", "text", false, UpdateTags(_)),
    renderInput("Seniority", "seniority", "text", false, UpdateSeniority(_)),
    renderInput("Other", "other", "text", false, UpdateOther(_)),
    button(`type` := "button", onClick(AttemptPostJob))("Post Job")
  )

  private def renderInvalidPage =
    div(
      h1("Post Job"),
      div("You need to be logged in to post a job")
    )

  // util
  private def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))
}

private object PostJobPage {
  trait Msg                                                 extends App.Msg
  private case class UpdateCompany(company: String)         extends Msg
  private case class UpdateTitle(title: String)             extends Msg
  private case class UpdateDescription(description: String) extends Msg
  private case class UpdateExternalUrl(externalUrl: String) extends Msg
  private case object ToggleRemote                          extends Msg
  private case class UpdateLocation(location: String)       extends Msg
  private case class UpdateSalaryLo(salaryLo: Int)          extends Msg
  private case class UpdateSalaryHi(salaryHi: Int)          extends Msg
  private case class UpdateCurrency(currency: String)       extends Msg
  private case class UpdateCountry(country: String)         extends Msg
  private case class UpdateTags(tags: String)               extends Msg
  private case class UpdateSeniority(seniority: String)     extends Msg
  private case class UpdateOther(other: String)             extends Msg
  private case object AttemptPostJob                        extends Msg
  private case class PostJobError(error: String)            extends Msg
  private case class PostJobSuccess(jobId: Any)             extends Msg

  object Endpoints {
    val postJob = new Endpoint[Msg] {
      override val location: String          = Constants.endpoints.postJob
      override val method: Method            = Method.Post
      override val onError: HttpError => Msg = e => PostJobError(e.toString)
      override val onResponse: Response => Msg = response =>
        response.status match {
          case Status(s, _) if s >= 200 && s < 300 =>
            val jobId = response.body
            PostJobSuccess(jobId)
          case Status(401, _) =>
            PostJobError("You are not authorized to post a job")
          case Status(s, _) if s >= 400 && s < 500 =>
            val json   = response.body
            val parsed = parse(json).flatMap(_.hcursor.get[String]("error"))
            parsed match {
              case Left(e)  => PostJobError(s"Error: $e")
              case Right(e) => PostJobError(e)
            }
          case _ =>
            PostJobError("Unknown reply from server. Something's fishy.")
        }
    }
  }

  object Commands {
    def postJob(
        company: String,
        title: String,
        description: String,
        externalUrl: String,
        location: String,
        remote: Boolean,
        salaryLo: Option[Int],
        salaryHi: Option[Int],
        currency: Option[String],
        country: Option[String],
        tags: Option[String],
        image: Option[String],
        seniority: Option[String],
        other: Option[String]
    ) =
      Endpoints.postJob.callAuthorized(
        JobInfo(
          company,
          title,
          description,
          externalUrl,
          location,
          remote,
          salaryLo,
          salaryHi,
          currency,
          country,
          tags.map(text => text.split(",").map(_.trim).toList),
          image,
          seniority,
          other
        )
      )
  }

}
