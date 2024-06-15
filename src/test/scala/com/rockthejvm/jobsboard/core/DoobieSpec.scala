package com.rockthejvm.jobsboard.core

import cats.effect.*
import doobie.util.transactor.Transactor
import doobie.implicits.*
import org.testcontainers.containers.PostgreSQLContainer
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor

trait DoobieSpec {

  // to be implemented by whatever test case interacts with the DB
  val initScript: String


  val postgres: Resource[IO, PostgreSQLContainer[Nothing]] = {
    val aquire = IO {
      val container: PostgreSQLContainer[Nothing] =
        new PostgreSQLContainer("postgres").withInitScript(initScript)
      container.start()
      container
    }
    val release = (container: PostgreSQLContainer[Nothing]) => IO(container.stop())
    Resource.make(aquire)(release)
  }

  // set up a Postgres transactor
  val transactor: Resource[IO, Transactor[IO]] = for {
    db <- postgres
    ce <- ExecutionContexts.fixedThreadPool[IO](1)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      db.getJdbcUrl(),
      db.getUsername(),
      db.getPassword(),
      ce
    )
  } yield xa
}
