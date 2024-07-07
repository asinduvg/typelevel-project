package com.rockthejvm.jobsboard.http.validation

import cats.*
import cats.implicits.*
import cats.data.*
import cats.data.Validated.*
import com.rockthejvm.jobsboard.domain.job.*
import com.rockthejvm.jobsboard.domain.user.*
import com.rockthejvm.jobsboard.domain.auth.*
import scala.util.{Try, Success, Failure}
import java.net.URL

object validators {

  sealed trait ValidationFailure(val errorMessage: String)
  case class EmptyField(fieldName: String) extends ValidationFailure(s"'$fieldName' is empty")
  case class InvalidUrl(fieldName: String)
      extends ValidationFailure(s"'$fieldName' is not a valid URL")
  case class InvalidEmail(fieldName: String)
      extends ValidationFailure(s"'$fieldName' is not a valid email")
  // empty field, invalid URL, invalid email

  type ValidationResult[A] = ValidatedNel[ValidationFailure, A]

  trait Validator[A] {
    def validate(value: A): ValidationResult[A]
  }

  val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  private def validateRequired[A](field: A, fieldName: String)(
      required: A => Boolean
  ): ValidationResult[A] =
    if (required(field)) field.validNel
    else EmptyField(fieldName).invalidNel

  private def validateUrl(field: String, fieldName: String): ValidationResult[String] =
    Try(URL(field).toURI()) match { // throws some exceptions
      case Success(_)         => field.validNel
      case Failure(exception) => InvalidUrl(fieldName).invalidNel
    }

  private def validateEmail(field: String, fieldName: String): ValidationResult[String] =
    if (emailRegex.findFirstMatchIn(field).isDefined) field.validNel
    else InvalidEmail(fieldName).invalidNel

  given jobInfoValidator: Validator[JobInfo] = (jobInfo: JobInfo) => {
    val JobInfo(
      company,     // should not be empty
      title,       // should not be empty
      description, // should not be empty
      externalUrl, // should be a valid URL
      location,    // should not be empty
      remote,
      salaryLo,
      salaryHi,
      currency,
      country,
      tags,
      image,
      seniority,
      other
    ) = jobInfo

    val validCompany     = validateRequired(company, "company")(_.nonEmpty)
    val validTitle       = validateRequired(title, "title")(_.nonEmpty)
    val validDescription = validateRequired(description, "description")(_.nonEmpty)
    val validExternalUrl = validateUrl(externalUrl, "external url")
    val validLocation    = validateRequired(location, "location")(_.nonEmpty)

    (
      validCompany,
      validTitle,
      validDescription,
      validExternalUrl,
      validLocation,
      remote.validNel,
      salaryLo.validNel,
      salaryHi.validNel,
      currency.validNel,
      country.validNel,
      tags.validNel,
      image.validNel,
      seniority.validNel,
      other.validNel
    ).mapN(JobInfo.apply) // ValidatedNel[ValidationFailure, JobInfo]
  }

  // create validators for
  // loginInfo
  given loginInfoValidator: Validator[LoginInfo] = (loginInfo: LoginInfo) => {
    val validUserEmail = validateRequired(loginInfo.email, "email")(_.nonEmpty)
      .andThen(e => validateEmail(e, "email"))
    val validPassword = validateRequired(loginInfo.password, "password")(_.nonEmpty)

    (
      validUserEmail,
      validPassword
    ).mapN(LoginInfo.apply)
  }
  // newPasswordInfo
  given newPasswordInfoValidator: Validator[NewPasswordInfo] = (newPasswordInfo: NewPasswordInfo) =>
    {
      val validOldPassword =
        validateRequired(newPasswordInfo.oldPassword, "old password")(_.nonEmpty)
      val validNewPassword =
        validateRequired(newPasswordInfo.newPassword, "new password")(_.nonEmpty)

      (
        validOldPassword,
        validNewPassword
      ).mapN(NewPasswordInfo.apply)
    }

  // newUserInfo
  given newUserInfoValidator: Validator[NewUserInfo] = (newUserInfo: NewUserInfo) => {
    val validUserEmail = validateRequired(newUserInfo.email, "email")(_.nonEmpty)
      .andThen(e => validateEmail(e, "email"))
    val validPassword = validateRequired(newUserInfo.password, "password")(_.nonEmpty)
    // ^^ you can run password validation logic here

    (
      validUserEmail,
      validPassword,
      newUserInfo.firstName.validNel,
      newUserInfo.lastName.validNel,
      newUserInfo.company.validNel
    ).mapN(NewUserInfo.apply)
  }
}
