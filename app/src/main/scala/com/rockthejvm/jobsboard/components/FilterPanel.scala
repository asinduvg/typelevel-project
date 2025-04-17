package com.rockthejvm.jobsboard.components

import cats.effect.IO
import com.rockthejvm.jobsboard.App
import com.rockthejvm.jobsboard.common.{Constants, Endpoint}
import com.rockthejvm.jobsboard.domain.job.JobFilter
import tyrian.*
import tyrian.Html.*
import tyrian.http.{HttpError, Method, Response}
import io.circe.generic.auto.*

case class FilterPanel(
    possibleFilters: JobFilter = JobFilter(),
    maybeError: Option[String] = None
) extends Component[App.Msg, FilterPanel] {
  import FilterPanel.*
  override def initCmd: Cmd[IO, App.Msg] = Commands.getFilters

  override def update(msg: App.Msg): (FilterPanel, Cmd[IO, App.Msg]) = msg match
    case SetPossibleFilters(possibleFilters) =>
      (this.copy(possibleFilters = possibleFilters), Cmd.None)
    case FilterPanelError(e) =>
      (this.copy(maybeError = Some(e)), Cmd.None)
    case _ => (this, Cmd.None)

  override def view: Html[App.Msg] =
    div(`class` := "filter-panel-container")(
      maybeRenderError(),
      div(possibleFilters.toString)
    )

  private def maybeRenderError() =
    maybeError
      .map { e =>
        div(`class` := "filter-panel-error")(e)
      }
      .getOrElse(div())
}

object FilterPanel {
  trait Msg                                                 extends App.Msg
  case class FilterPanelError(error: String)                extends Msg
  case class SetPossibleFilters(possibleFilters: JobFilter) extends Msg

  object Endpoints {
    val getFilters = new Endpoint[Msg] {
      override val location: String          = Constants.endpoints.filters
      override val method: Method            = Method.Get
      override val onError: HttpError => Msg = e => FilterPanelError(e.toString)
      override val onResponse: Response => Msg = Endpoint.onResponse[JobFilter, Msg](
        SetPossibleFilters(_),
        FilterPanelError(_)
      )
    }
  }

  object Commands {
    def getFilters = Endpoints.getFilters.call

  }
}
