package com.rockthejvm.jobsboard.fixtures

import com.rockthejvm.jobsboard.domain.user.*

trait UserFixture {
  val Daniel = User(
    "daniel@rockthejvm.com",
    "$2a$10$nVEJ3pJkjN1K6esp6aS6s.0mp2gGMep1x7Akaz3UgzTCrxGnAwC0a",
    Some("Daniel"),
    Some("Ciocirlan"),
    Some("Rock the JVM"),
    Role.ADMIN
  )

  val danielEmail    = Daniel.email
  val danielPassword = "rockthejvm"

  val Riccardo = User(
    "riccardo@rockthejvm.com",
    "$2a$10$VgVDCMWMsS2F1FXn/dfZ.uBgRyVOao833QyQXy0Sn8/T9NqnRC7Mu",
    Some("Riccardo"),
    Some("Cardin"),
    Some("Rock the JVM"),
    Role.RECRUITER
  )

  val riccardoEmail    = Riccardo.email
  val riccardoPassword = "riccardorulez"

  val UpdatedRiccardo = User(
    "riccardo@rockthejvm.com",
    "$2a$10$Mj9EsiXeajA4akx2KwORtutflQ099uQYoGTX2m9ERlFjmhlKtngbq",
    Some("RICCARDO"),
    Some("CARDIN"),
    Some("Adobe"),
    Role.RECRUITER
  )

  val NewUser = User(
    "newuser@gmail.com",
    "$2a$10$E/2Cjo8hKzeAhhY2lCMTzODu8ig7MWchuweql2/ElwRgsRUEhDVEi",
    Some("John"),
    Some("Doe"),
    Some("Some company"),
    Role.RECRUITER
  )

  val NewUserDaniel = NewUserInfo(
    danielEmail,
    danielPassword,
    Some("Daniel"),
    Some("Ciocirlan"),
    Some("Rock the JVM")
  )
  
  val NewUserRiccardo = NewUserInfo(
    riccardoEmail,
    riccardoPassword,
    Some("Riccardo"),
    Some("Cardin"),
    Some("Rock the JVM")
  )
}
