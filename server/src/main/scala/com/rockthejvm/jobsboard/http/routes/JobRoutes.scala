package com.rockthejvm.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.*
import cats.effect.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.Http

import java.net.http.HttpRequest
import cats.syntax.all.*
import tsec.authentication.{SecuredRequestHandler, asAuthed}

import java.util.UUID
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.domain.security.*
import com.rockthejvm.jobsboard.http.responses.FailureResponse
import org.typelevel.log4cats.Logger
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.logging.syntax.*
import org.http4s.HttpRoutes
import com.rockthejvm.jobsboard.http.validation.syntax.*
import com.rockthejvm.jobsboard.domain.pagination.*
import org.typelevel.ci.CIStringSyntax

import scala.language.implicitConversions

class JobRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (jobs: Jobs[F], stripe: Stripe[F])
    extends HttpValidationDsl[F] {

  object OffsetQueryParam   extends OptionalQueryParamDecoderMatcher[Int]("offset")
  object LimitsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("limit")

  // GET /jobs/filters => { filters }
  private val allFiltersRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "filters" =>
    jobs.possibleFilters().flatMap(jf => Ok(jf))
  }

  // POST /jobs?limit=x&offset=y { filters }
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root :? LimitsetQueryParam(limit) +& OffsetQueryParam(offset) =>
      for {
        filter <- req.as[JobFilter]
        jobs   <- jobs.all(filter, Pagination(limit, offset))
        resp   <- Ok(jobs)
      } yield resp
  }

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(uuid) =>
    for {
      job <- jobs.find(uuid)
      resp <- job match
        case Some(job) => Ok(job)
        case None      => NotFound(FailureResponse(s"Job with UUID $uuid not found"))
    } yield resp
  }

  // POST /jobs/create { jobsInfo }
  private val createJobRoute: AuthRoute[F] = { case req @ POST -> Root / "create" asAuthed user =>
    req.request.validate[JobInfo] { jobInfo =>
      for {
        id   <- jobs.create(user.email, jobInfo)
        resp <- Created(id)
      } yield resp
    }
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: AuthRoute[F] = {
    case req @ PUT -> Root / UUIDVar(uuid) asAuthed user =>
      req.request.validate[JobInfo] { jobInfo =>
        jobs.find(uuid).flatMap {
          case None =>
            NotFound(FailureResponse(s"Cannot update job $uuid: not found"))
          case Some(job) if user.owns(job) || user.isAdmin =>
            jobs.update(uuid, jobInfo) *> Ok()
          case _ => Forbidden(FailureResponse("You can only update your own jobs"))
        }
      }
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: AuthRoute[F] = { case DELETE -> Root / UUIDVar(uuid) asAuthed user =>
    jobs.find(uuid).flatMap {
      case None => NotFound(FailureResponse(s"Cannot delete job $uuid: not found"))
      case Some(job) if user.owns(job) || user.isAdmin =>
        jobs.delete(uuid) *> Ok()
      case _ => Forbidden(FailureResponse("You can only delete your own jobs"))
    }
  }

  // Stripe endpoints
  // POST /jobs/promoted { jobInfo }
  private val promotedJobRoute: AuthRoute[F] = {
    case req @ POST -> Root / "promoted" asAuthed user =>
      req.request.validate[JobInfo] { jobInfo =>
        for {
          jobId   <- jobs.create(user.email, jobInfo)
          session <- stripe.createCheckoutSession(jobId.toString, user.email)
          resp    <- session.map(sesh => Ok(sesh.getUrl())).getOrElse(NotFound())
        } yield resp
      }
  }

  private val promotedJobWebhook: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "webhook" =>
      val stripeSigHeader =
        req.headers.get(ci"Stripe-Signature").flatMap(_.toList.headOption).map(_.value)
      stripeSigHeader match {
        case Some(signature) =>
          for {
            payload <- req.bodyText.compile.string
            handled <- stripe.handleWebhookEvent(
              payload,
              signature,
              jobId => jobs.activate(UUID.fromString(jobId))
            ) // TODO
            resp <- if (handled.nonEmpty) Ok() else NoContent()
          } yield resp

        case None =>
          Logger[F].info("Got webhook event with no Stripe signature") *>
            Forbidden("No Stripe signature")
      }
  }

  val unauthedRoutes =
    allFiltersRoute <+> allJobsRoute <+> findJobRoute <+> promotedJobWebhook
  val authedRoutes = SecuredHandler[F].liftService(
    createJobRoute.restrictedTo(adminOnly) |+|
      promotedJobRoute.restrictedTo(allRoles) |+|
      updateJobRoute.restrictedTo(allRoles) |+|
      deleteJobRoute.restrictedTo(allRoles)
  )

  val routes = Router(
    "/jobs" -> (unauthedRoutes <+> authedRoutes)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger: SecuredHandler](
      jobs: Jobs[F],
      stripe: Stripe[F]
  ): JobRoutes[F] = new JobRoutes[F](jobs, stripe)
}
