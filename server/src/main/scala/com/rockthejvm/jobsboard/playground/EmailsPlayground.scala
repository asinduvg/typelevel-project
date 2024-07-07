package com.rockthejvm.jobsboard.playground

import java.util.Properties
import javax.mail.{Authenticator, PasswordAuthentication, Session, Message, Transport}
import javax.mail.internet.MimeMessage
import cats.effect.IOApp
import cats.effect.IO
import com.rockthejvm.jobsboard.core.*
import com.rockthejvm.jobsboard.config.*

object EmailsPlayground {
  def main(args: Array[String]): Unit = {
    // configs
    val host        = "smtp.ethereal.email"
    val port        = 587
    val user        = "jena.gibson@ethereal.email"
    val pass        = "TVjypk7q2gCcRYvyY6"
    val token       = "ABCD1234"
    val frontendUrl = "https://google.com"

    // properties file
    val prop = new Properties
    prop.put("mail.smtp.auth", "true")
    prop.put("mail.smtp.starttls.enable", "true")
    prop.put("mail.smtp.host", host)
    prop.put("mail.smtp.port", port)
    prop.put("mail.smtp.ssl.trust", host)

    // authentication
    val auth = new Authenticator {
      override protected def getPasswordAuthentication: PasswordAuthentication =
        new PasswordAuthentication(user, pass)
    }

    // session
    val session = Session.getInstance(prop, auth)

    // email itselt
    val subject = "Email from Rock the JVM"
    val content = s"""
        <div style="
            border: 1px solid black;
            padding: 20px;
            font-family: sans-serif;
            font-size: 20px;
            line-height: 2;
        ">
            <h1>Rock the JVM: Password Recovery</h1>
            <p> Your password recovery token is: <strong>$token</strong> </p>
            <p> Click <a href="$frontendUrl/login">here</a> to get back to the application. </p>
        </div>
        <p> 😘 from Rock the JVM </p>
    """

    // message = MIME message
    val message = new MimeMessage(session)
    message.setFrom("daniel@rockthejvm.com")
    message.setRecipients(Message.RecipientType.TO, "the.user@gmail.com")
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")

    // send email
    Transport.send(message)
  }
}

object EmailsEffectPlayground extends IOApp.Simple {
  override def run: IO[Unit] = for {
    emails <- LiveEmails[IO](
      EmailServiceConfig(
        host = "smtp.ethereal.email",
        port = 587,
        user = "jena.gibson@ethereal.email",
        pass = "TVjypk7q2gCcRYvyY6",
        frontendUrl = "https://google.com"
      )
    )
    _ <- emails.sendPasswordRecoveryEmail("someone@rockthejvm.com", "ROCKTJVM")
  } yield ()
}
