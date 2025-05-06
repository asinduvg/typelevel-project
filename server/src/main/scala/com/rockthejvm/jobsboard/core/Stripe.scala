package com.rockthejvm.jobsboard.core

import cats.MonadThrow
import cats.implicits.*
import com.rockthejvm.jobsboard.config.StripeConfig
import com.stripe.Stripe as TheStripe
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams
import com.rockthejvm.jobsboard.logging.syntax.*
import org.typelevel.log4cats.Logger

trait Stripe[F[_]] {
  def createCheckoutSession(jobId: String, userEmail: String): F[Option[Session]]
}

class LiveStripe[F[_]: MonadThrow: Logger](
    key: String,
    price: String,
    successUrl: String,
    cancelUrl: String
) extends Stripe[F] {
  // globally set constant
  TheStripe.apiKey = key

  override def createCheckoutSession(jobId: String, userEmail: String): F[Option[Session]] =
    SessionCreateParams
      .builder()
      .setMode(SessionCreateParams.Mode.PAYMENT)
      .setInvoiceCreation(
        SessionCreateParams.InvoiceCreation.builder().setEnabled(true).build()
      )
      .setPaymentIntentData(
        SessionCreateParams.PaymentIntentData.builder().setReceiptEmail(userEmail).build()
      )
      .setSuccessUrl(s"$successUrl/$jobId")
      .setCancelUrl(s"$cancelUrl")
      .setCustomerEmail(userEmail)
      .setClientReferenceId(jobId) // will be sent back to me by the webhook
      .addLineItem(
        SessionCreateParams.LineItem
          .builder()
          .setQuantity(1L)
          .setPrice(price)
          .build()
      )
      .build()
      .pure[F]
      .map(params => Session.create(params))
      .map(_.some)
      .logError(error => s"Creating checkout session failed: $error")
      .recover { case _ => None }
}

object LiveStripe {
  def apply[F[_]: MonadThrow: Logger](stripeConfig: StripeConfig) = new LiveStripe[F](
    stripeConfig.key,
    stripeConfig.price,
    stripeConfig.successUrl,
    stripeConfig.cancelUrl
  ).pure[F]
}
