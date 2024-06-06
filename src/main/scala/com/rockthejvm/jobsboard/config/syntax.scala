package com.rockthejvm.jobsboard.config

import pureconfig.ConfigSource
import cats.MonadThrow
import pureconfig.ConfigReader
import pureconfig.error.ConfigReaderException
import cats.implicits.*
import scala.reflect.ClassTag

object syntax {
  extension (source: ConfigSource)
    def loadF[F[_], A](using
        configReader: ConfigReader[A],
        F: MonadThrow[F],
        tag: ClassTag[A]
    ): F[A] =
      F.pure(source.load[A]).flatMap {
        case Left(error)   => F.raiseError[A](ConfigReaderException(error))
        case Right(config) => F.pure(config)
      }
}
