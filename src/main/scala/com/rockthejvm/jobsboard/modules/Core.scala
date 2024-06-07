package com.rockthejvm.jobsboard.modules

import com.rockthejvm.jobsboard.core.Jobs
import cats.effect.kernel.Resource
import cats.effect.*
import cats.implicits.*
import com.rockthejvm.jobsboard.core.LiveJobs
import doobie.util.transactor.Transactor

final class Core[F[_]] private (val jobs: Jobs[F])

// postgres -> jobs -> core -> httpApi -> app
object Core {
  def apply[F[_]: Async](xa: Transactor[F]): Resource[F, Core[F]] =
    Resource
      .eval(LiveJobs[F](xa))
      .map(jobs => new Core(jobs))
}
