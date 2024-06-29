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
        .flatMap(IO.println) *>
      BCrypt.hashpw[IO]("rockthejvm").flatMap(IO.println) *>
      BCrypt
        .checkpwBool[IO](
          "rockthejvm",
          PasswordHash[BCrypt]("$2a$10$nVEJ3pJkjN1K6esp6aS6s.0mp2gGMep1x7Akaz3UgzTCrxGnAwC0a")
        )
        .flatMap(IO.println) *>
      BCrypt.hashpw[IO]("riccardorulez").flatMap(IO.println) *>
      BCrypt
        .checkpwBool[IO](
          "riccardorulez",
          PasswordHash[BCrypt]("$2a$10$VgVDCMWMsS2F1FXn/dfZ.uBgRyVOao833QyQXy0Sn8/T9NqnRC7Mu")
        )
        .flatMap(IO.println) *>
      BCrypt.hashpw[IO]("riccardorocks").flatMap(IO.println) *>
      BCrypt
        .checkpwBool[IO](
          "riccardorocks",
          PasswordHash[BCrypt]("$2a$10$Mj9EsiXeajA4akx2KwORtutflQ099uQYoGTX2m9ERlFjmhlKtngbq")
        )
        .flatMap(IO.println) *>
      BCrypt.hashpw[IO]("simplepassword").flatMap(IO.println) *>
      BCrypt
        .checkpwBool[IO](
          "simplepassword",
          PasswordHash[BCrypt]("$2a$10$E/2Cjo8hKzeAhhY2lCMTzODu8ig7MWchuweql2/ElwRgsRUEhDVEi")
        )
        .flatMap(IO.println)
}
