package com.rockthejvm.jobsboard.http

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import org.http4s.HttpRoutes
import cats.*
import cats.effect.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.Http
import java.net.http.HttpRequest
import cats.syntax.all.*
import scala.collection.mutable

import java.util.UUID
import com.rockthejvm.jobsboard.domain.Job.*
import com.rockthejvm.jobsboard.http.responses.FailureResponse
import org.typelevel.log4cats.Logger

class JobRoutes[F[_]: Concurrent: Logger] extends Http4sDsl[F] {

  // "database"
  private val database = mutable.Map.empty[UUID, Job]

  // POST /jobs?offset=x&limit=y { filters } // TODO: add query params and filters
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] { case POST -> Root =>
    Ok(database.values)
  }

  // GET /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(uuid) =>
    database.get(uuid) match
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Job with UUID $uuid not found"))
  }

  // POST /jobs/create { jobsInfo }
  private def createJob(jobInfo: JobInfo): F[Job] =
    Job(
      id = UUID.randomUUID(),
      date = System.currentTimeMillis(),
      ownerEmail = "todo@rockthejvm.com",
      jobInfo = jobInfo,
      active = true
    ).pure[F]

  import com.rockthejvm.jobsboard.logging.syntax.*
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      for {
        _       <- Logger[F].info("Trying to add job") 
        jobInfo <- req.as[JobInfo].logError(e => s"Parsing payload failed: $e")
        _       <- Logger[F].info(s"Parsed job info: $jobInfo")
        job     <- createJob(jobInfo)
        _       <- Logger[F].info(s"Created job info: $job")
        _       <- database.put(job.id, job).pure[F]
        resp    <- Created(job.id)
      } yield resp
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(uuid) =>
      database.get(uuid) match
        case None => NotFound(FailureResponse(s"Cannot update job $uuid: not found"))
        case Some(job) =>
          for {
            jobInfo <- req.as[JobInfo]
            _       <- database.put(uuid, job.copy(jobInfo = jobInfo)).pure[F]
            resp    <- Ok()
          } yield resp
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case DELETE -> Root / UUIDVar(uuid) =>
      database.get(uuid) match
        case None => NotFound(FailureResponse("Cannot delete job $uuid: not found"))
        case Some(job) =>
          database.remove(uuid)
          Ok()
  }

  val routes = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger]: JobRoutes[F] = new JobRoutes[F]
}
