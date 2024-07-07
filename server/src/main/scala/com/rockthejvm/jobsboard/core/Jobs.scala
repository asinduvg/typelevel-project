package com.rockthejvm.jobsboard.core

import cats.*
import cats.implicits.*
import cats.effect.*
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.domain.pagination.*
import com.rockthejvm.jobsboard.logging.syntax.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*
import java.util.UUID
import doobie.util.fragment.Fragment
import org.typelevel.log4cats.Logger

trait Jobs[F[_]] {
  // "algebra"
  // CRUD
  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]
  def all(): F[List[Job]]
  def all(filter: JobFilter, pagination: Pagination): F[List[Job]]
  def find(id: UUID): F[Option[Job]]
  def update(id: UUID, jobInfo: JobInfo): F[Option[Job]]
  def delete(id: UUID): F[Int]
}

class LiveJobs[F[_]: MonadCancelThrow: Logger] private (xa: Transactor[F]) extends Jobs[F] {
  override def create(ownerEmail: String, jobInfo: JobInfo): F[UUID] =
    sql"""
        INSERT INTO jobs(date, ownerEmail, company, title, description, externalUrl, location, remote, salaryLo, salaryHi, currency, country, tags, image, seniority, other, active)
        VALUES (${System
        .currentTimeMillis()}, ${ownerEmail}, ${jobInfo.company}, ${jobInfo.title}, ${jobInfo.description}, ${jobInfo.externalUrl},
            ${jobInfo.location}, ${jobInfo.remote}, ${jobInfo.salaryLo}, ${jobInfo.salaryHi}, ${jobInfo.currency}, ${jobInfo.country}, ${jobInfo.tags},
            ${jobInfo.image}, ${jobInfo.seniority}, ${jobInfo.other}, false
        )
    """.update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(xa)

  override def all(): F[List[Job]] =
    sql"""
        SELECT id, date, ownerEmail, company, title, description, externalUrl, location, remote, salaryLo, salaryHi, currency, country, tags, image, seniority, other, active
        FROM jobs
    """
      .query[Job]
      .to[List]
      .transact(xa)

  override def all(filter: JobFilter, pagination: Pagination): F[List[Job]] = {
    val selectFragment: Fragment =
      fr"""
          SELECT id, date, ownerEmail, company, title, description, externalUrl, location, remote, salaryLo, salaryHi, currency, country, tags, image, seniority, other, active
      """

    val fromFragment: Fragment =
      fr"FROM jobs"

    val whereFragment: Fragment = Fragments.whereAndOpt(
      filter.companies.toNel.map(companies => Fragments.in(fr"company", companies)),
      filter.locations.toNel.map(locations => Fragments.in(fr"location", locations)),
      filter.countries.toNel.map(countries => Fragments.in(fr"country", countries)),
      filter.seniorities.toNel.map(seniorities => Fragments.in(fr"seniority", seniorities)),
      filter.tags.toNel.map(tags => // intersection between filter.tags and row's tags
        Fragments.or(tags.toList.map(tag => fr"$tag=any(tags)"): _*)
      ),
      filter.maxSalary.map(salary => fr"salaryHi > $salary"),
      filter.remote.some.map(remote => fr"remote=$remote")
    )
    /*
      WHERE company in [filter.companies]
      AND location in [filter.locations]
      AND country in [filter.countries]
      AND seniority in [filter.seniorities]
      AND (
        tag1=any(tags)
        OR tag2=any(tags)
        OR ... (for every tag in filter.tags)
      )
      AND salaryHi > [filter.salary]
      AND remote = [filter.remote]
     */

    val paginationFragment: Fragment =
      fr"ORDER BY id LIMIT ${pagination.limit} OFFSET ${pagination.offset}"

    val statement = selectFragment |+| fromFragment |+| whereFragment |+| paginationFragment

    Logger[F].info(statement.toString) *>
      statement
        .query[Job]
        .to[List]
        .transact(xa)
        .logError(e => s"Failed query: ${e.getMessage}")

  }

  override def find(id: UUID): F[Option[Job]] =
    sql"""
        SELECT id, date, ownerEmail, company, title, description, externalUrl, location, remote, salaryLo, salaryHi, currency, country, tags, image, seniority, other, active
        FROM jobs
        WHERE id = $id
    """
      .query[Job]
      .option
      .transact(xa)

  override def update(id: UUID, jobInfo: JobInfo): F[Option[Job]] =
    sql"""
        UPDATE jobs
        SET company = ${jobInfo.company}, title = ${jobInfo.title}, description = ${jobInfo.description}, externalUrl = ${jobInfo.externalUrl},
            location = ${jobInfo.location}, remote = ${jobInfo.remote}, salaryLo = ${jobInfo.salaryLo}, salaryHi = ${jobInfo.salaryHi}, currency = ${jobInfo.currency},
            country = ${jobInfo.country}, tags = ${jobInfo.tags}, image = ${jobInfo.image}, seniority = ${jobInfo.seniority}, other = ${jobInfo.other}
        WHERE id = $id
    """.update.run
      .transact(xa)
      .flatMap(_ => find(id))

  override def delete(id: UUID): F[Int] =
    sql"""
        DELETE FROM jobs
        WHERE id = $id
    """.update.run
      .transact(xa)
}

object LiveJobs {
  def apply[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): F[LiveJobs[F]] = new LiveJobs[F](xa).pure[F]
}
