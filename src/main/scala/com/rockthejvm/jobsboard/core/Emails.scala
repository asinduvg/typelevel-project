package com.rockthejvm.jobsboard.core

import cats.effect.*
import cats.implicits.*
import com.rockthejvm.jobsboard.config.EmailServiceConfig
import java.util.Properties
import javax.mail.{Authenticator, PasswordAuthentication, Session, Message, Transport}
import javax.mail.internet.MimeMessage

trait Emails[F[_]] {
  def sendEmail(to: String, subject: String, content: String): F[Unit]
  def sendPasswordRecoveryEmail(to: String, token: String): F[Unit]
}

class LiveEmails[F[_]: MonadCancelThrow] private (emailServiceConfig: EmailServiceConfig)
    extends Emails[F] {

  val host        = emailServiceConfig.host
  val port        = emailServiceConfig.port
  val user        = emailServiceConfig.user
  val pass        = emailServiceConfig.pass
  val frontendUrl = emailServiceConfig.frontendUrl

  override def sendEmail(to: String, subject: String, content: String): F[Unit] =
    val messageResource = for {
      props   <- propsResource
      auth    <- authenticatorResource
      session <- createSession(props, auth)
      message <- createMessage(session)("daniel@rockthejvm.com", to, subject, content)
    } yield message

    messageResource.use(msg => Transport.send(msg).pure[F])

  override def sendPasswordRecoveryEmail(to: String, token: String): F[Unit] = {
    val subject = "Rock the JVM: Password Recovery"
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

    sendEmail(to, subject, content)
  }

  val propsResource: Resource[F, Properties] =
    val props = new Properties()
    props.put("mail.smtp.auth", true)
    props.put("mail.smtp.starttls.enable", true)
    props.put("mail.smtp.host", host)
    props.put("mail.smtp.port", port)
    props.put("mail.smtp.ssl.trust", host)
    Resource.pure(props)

  val authenticatorResource: Resource[F, Authenticator] = Resource.pure {
    new Authenticator {
      override protected def getPasswordAuthentication: PasswordAuthentication =
        new PasswordAuthentication(user, pass)
    }
  }

  def createSession(props: Properties, auth: Authenticator): Resource[F, Session] =
    val session = Session.getInstance(props, auth)
    Resource.pure(session)

  def createMessage(
      session: Session
  )(from: String, to: String, subject: String, content: String): Resource[F, MimeMessage] =
    val message = new MimeMessage(session)
    message.setFrom(from)
    message.setRecipients(Message.RecipientType.TO, to)
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")
    Resource.pure(message)

}

object LiveEmails {
  def apply[F[_]: MonadCancelThrow](emailServiceConfig: EmailServiceConfig): F[Emails[F]] =
    new LiveEmails[F](emailServiceConfig).pure[F]
}
