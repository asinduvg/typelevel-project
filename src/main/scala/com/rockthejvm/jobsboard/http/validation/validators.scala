package com.rockthejvm.jobsboard.http.validation

import cats.* 
import cats.implicits.*
import cats.data.* 
import cats.data.Validated.* 
import com.rockthejvm.jobsboard.domain.job.JobInfo
import scala.util.{Try, Success, Failure}
import java.net.URL

object validators {

  sealed trait ValidationFailure(val errorMessage: String)
  case class EmptyField(fieldName: String) extends ValidationFailure(s"'$fieldName' is empty")
  case class InvalidUrl(fieldName: String) extends ValidationFailure(s"'$fieldName' is not a valid URL")
  // empty field, invalid URL, invalid email

  type ValidationResult[A] = ValidatedNel[ValidationFailure, A]

  trait Validator[A] {
    def validate(value: A): ValidationResult[A]
  }

  given jobInfoValidator: Validator[JobInfo] = (jobInfo: JobInfo) => {
    val JobInfo(
      company, // should not be empty
      title, // should not be empty
      description, // should not be empty
      externalUrl, // should be a valid URL
      location, // should not be empty
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

    val validCompany = validateRequired(company, "company")(_.nonEmpty)
    val validTitle = validateRequired(title, "title")(_.nonEmpty)
    val validDescription = validateRequired(description, "description")(_.nonEmpty)
    val validExternalUrl = validateUrl(externalUrl, "externalUrl")
    val validLocation = validateRequired(location, "location")(_.nonEmpty)

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

  private def validateRequired[A]
  (field: A, fieldName: String)
  (required: A => Boolean): ValidationResult[A] =
    if (required(field)) field.validNel
    else EmptyField(fieldName).invalidNel

  private def validateUrl(field: String, fieldName: String): ValidationResult[String] =
    Try(URL(field).toURI()) match { // throws some exceptions
      case Success(_) => field.validNel
      case Failure(exception) => InvalidUrl(fieldName).invalidNel
    } 
}
