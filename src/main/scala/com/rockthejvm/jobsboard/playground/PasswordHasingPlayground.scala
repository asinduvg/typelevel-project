package com.rockthejvm.jobsboard.playground

import cats.effect.*
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash

object PasswordHasingPlayground extends IOApp.Simple {
  override def run: IO[Unit] =
    BCrypt.hashpw[IO]("scalarocks").flatMap(IO.println) *>
      BCrypt
        .checkpwBool[IO](
          "scalarocks",
          PasswordHash[BCrypt]("$2a$10$a/w7vQfu2biTEuNMNU.0Z.azOx03aDuU46GT0UcCz69getDWzBB.q")
        )
        .flatMap(IO.println)
}
