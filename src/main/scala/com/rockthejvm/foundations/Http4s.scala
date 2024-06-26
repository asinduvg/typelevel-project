package com.rockthejvm.foundations

import cats.*
import cats.implicits.*
import cats.effect.*
import io.circe.syntax.*
import io.circe.generic.auto.*
import org.typelevel.ci.CIString
import java.util.UUID
import org.http4s.circe.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import com.comcast.ip4s._
import org.http4s.headers.*

object Http4s extends IOApp.Simple {

  // simulate an HTTP server with "students" and "courses"
  type Student = String
  case class Instructor(firstName: String, lastName: String)
  case class Course(
      id: String,
      title: String,
      year: Int,
      students: List[Student],
      instructorName: String
  )

  object CourseRepository {
    // a "database"

    private val catsEffectCourse = Course(
      "110c68a3-f3c7-4356-b094-6014676d9ae7",
      "Rock the JVM Ultimate Scala Course",
      2022,
      List("Daniel", "Master Yoda"),
      "Martin Odersky"
    )

    private val courses: Map[String, Course] = Map(catsEffectCourse.id -> catsEffectCourse)

    // API
    def findCoursesById(courseId: UUID): Option[Course] =
      courses.get(courseId.toString)

    def findCoursesByInstructor(name: String): List[Course] =
      courses.values.filter(_.instructorName == name).toList
  }

  // essential REST endpoints
  // GET localhost:8080/courses?instructor=Martin%20Odersky&year=2022
  // GET localhost:8080/courses/110c68a3-f3c7-4356-b094-6014676d9ae7/students

  object InstructorQueryParamMatcher extends QueryParamDecoderMatcher[String]("instructor")
  object YearQueryParamMatcher       extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")

  def courseRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "courses" :? InstructorQueryParamMatcher(
            instructor
          ) +& YearQueryParamMatcher(maybeYear) =>
        val courses = CourseRepository.findCoursesByInstructor(instructor)
        maybeYear match {
          case Some(y) =>
            y.fold(
              _ => BadRequest("Parameter 'year' is invalid"),
              year => Ok(courses.filter(_.year == year).asJson)
            )
          case None => Ok(courses.asJson)
        }
      case GET -> Root / "courses" / UUIDVar(courseId) / "students" =>
        CourseRepository.findCoursesById(courseId).map(_.students) match {
          case Some(students) =>
            Ok(students.asJson, Header.Raw(CIString("My-custom-header"), "rockthejvm"))
          case None => NotFound(s"No course with $courseId was found")
        }
    }
  }

  def healthEndpoint[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl.*
    HttpRoutes.of[F] { case GET -> Root / "health" =>
      Ok("All going good!")
    }
  }

  def allRoutes[F[_]: Monad]: HttpRoutes[F] = courseRoutes[F] <+> healthEndpoint[F]

  def routerWithPathPrefixes = Router(
    "/api"     -> courseRoutes[IO],
    "/private" -> healthEndpoint[IO]
  ).orNotFound

  override def run = EmberServerBuilder
    .default[IO]
    .withHttpApp(routerWithPathPrefixes)
    .withPort(port"9000")
    .build
    .use(_ => IO.println("Server ready!") *> IO.never)
}
