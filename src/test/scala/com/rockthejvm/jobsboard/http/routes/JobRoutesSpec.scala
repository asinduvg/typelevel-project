package com.rockthejvm.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import cats.effect.*
import cats.implicits.*
import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.dsl.*
import org.scalatest.matchers.should.Matchers

import com.rockthejvm.jobsboard.domain.Job.*
import com.rockthejvm.jobsboard.core.Jobs
import com.rockthejvm.jobsboard.fixtures.JobFixture
import java.util.UUID
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class JobRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with JobFixture {
  //////////////////////////////////////////////////////////////////////
  // PREP
  //////////////////////////////////////////////////////////////////////
  val jobs: Jobs[IO] = new Jobs[IO] {
    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] =
      IO.pure(NewJobUuid)

    override def all(): IO[List[Job]] =
      IO.pure(List(AwesomeJob))

    override def find(id: UUID): IO[Option[Job]] =
      if (id == AwesomeJobUuid)
        println("hiiiii")
        IO.pure(Some(AwesomeJob))
      else
        println("hiiiii2")
        IO.pure(None)

    override def update(id: UUID, jobInfo: JobInfo): IO[Option[Job]] =
      if (id == AwesomeJobUuid)
        IO.pure(Some(UpdatedAwesomeJob))
      else
        IO.pure(None)
    override def delete(id: UUID): IO[Int] =
      if (id == AwesomeJobUuid)
        IO.pure(1)
      else
        IO.pure(0)
  }

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  // this is what we are testing
  val jobRoutes: HttpRoutes[IO] = JobRoutes[IO](jobs).routes

  //////////////////////////////////////////////////////////////////////
  // tests
  //////////////////////////////////////////////////////////////////////
  "JobRoutes" - {
    "should return a job with a given id" in {
      // code under test
      for {
        // simulate an HTTP request
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
        )
        retrieved <- response.as[Job]
        // get the HTTP response
        // make some assertions
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe AwesomeJob
      }
    }

    "should return all jobs" in {
      // code under test
      for {
        // simulate an HTTP request
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs/")
        )
        retrieved <- response.as[List[Job]]
        // get the HTTP response
        // make some assertions
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List(AwesomeJob)
      }
    }

    "should create a new job" in {
      // code under test
      for {
        // simulate an HTTP request
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs/create")
            .withEntity(AwesomeJob.jobInfo)
        )
        retrieved <- response.as[UUID]
        // get the HTTP response
        // make some assertions
      } yield {
        response.status shouldBe Status.Created
        retrieved shouldBe NewJobUuid
      }
    }

    "should only update a job that exists" in {
      // code under test
      for {
        // simulate an HTTP request
        responseOk <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
        responseInvalid <- jobRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-000000000000")
            .withEntity(UpdatedAwesomeJob.jobInfo)
        )
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }

    "should only delete a job that exists" in {
      // code under test
      for {
        // simulate an HTTP request
        responseOk <- jobRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
        )
        responseInvalid <- jobRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-000000000000")
        )
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }
  }

}
