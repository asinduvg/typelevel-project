package com.rockthejvm.jobsboard.components

import cats.effect.IO
import tyrian.{Cmd, Html}

import tyrian.*
import tyrian.Html.*
import cats.effect.IO

trait Component[Msg, +Model] {
  def initCmd: Cmd[IO, Msg]

  def update(msg: Msg): (Model, Cmd[IO, Msg])

  def view: Html[Msg]
}
