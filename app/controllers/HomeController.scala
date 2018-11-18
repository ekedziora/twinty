package controllers

import java.io.File
import java.nio.file.FileSystem
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject._

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.actor.Actor.Receive
import akka.stream._
import akka.stream.scaladsl.{Flow, Framing, Sink, Source}
import akka.util.ByteString
import io.netty.handler.codec.http.HttpMethod
import model.Tweet
import play.api.http.ContentTypes
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.{EventSource, concurrent}
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee.{Enumeratee, Enumerator, Iteratee}
import play.api.libs.json._
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.streams.ActorFlow
import play.api.libs.ws.{StreamedResponse, WSClient}
import play.api.mvc._
import play.api.Logger

import scala.sys.process._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.util.{Failure, Try}

@Singleton
class HomeController @Inject() (val messagesApi: MessagesApi, val ws: WSClient, val configuration: play.api.Configuration) extends Controller with I18nSupport {

  val logger: Logger = Logger("Stream logger")

  val key: ConsumerKey = ConsumerKey(configuration.underlying.getString("twitter.consumer.key"), configuration.underlying.getString("twitter.consumer.secret"))
  val token: RequestToken = RequestToken(configuration.underlying.getString("twitter.request.token"), configuration.underlying.getString("twitter.request.secret"))

  val decider: Supervision.Decider = { e =>
    logger.error("Unhandled exception in stream", e)
    Supervision.Stop
  }

  implicit val system = ActorSystem("twinty")
  val materializerSettings = ActorMaterializerSettings(system).withSupervisionStrategy(decider)
  implicit val materializer = ActorMaterializer(materializerSettings)(system)

  implicit val readsDatetime = Reads.localDateTimeReads(DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH))

  implicit val writesDatetime = Writes[LocalDateTime](datetime => JsString(DateTimeFormatter.ISO_DATE_TIME.format(datetime)))

  implicit val reads: Reads[Tweet] = (
      (JsPath \ "text").read[String] and
      (JsPath \ "user" \ "name").read[String] and
      (JsPath \ "created_at").read[LocalDateTime](readsDatetime) and
      (JsPath \ "favorite_count").read[Int]
    )(Tweet.apply _)

  implicit val writes: Writes[Tweet] = (
      (JsPath \ "text").write[String] and
      (JsPath \ "userName").write[String] and
      (JsPath \ "creationDateTime").write[LocalDateTime] and
      (JsPath \ "favorites").write[Int]
    )(unlift(Tweet.unapply))

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def search = Action {
    Ok.chunked(initRequest() via EventSource.flow).as(ContentTypes.EVENT_STREAM).withHeaders("Content-Encoding" -> "identity")
  }

  private def initRequest() = {
    val response = ws.url("https://stream.twitter.com/1.1/statuses/sample.json")
      .withQueryString("language" -> "en")
      .sign(OAuthCalculator(key, token))
      .withMethod(HttpMethod.GET.name())
      .stream()

    Source.fromFuture(response)
      .flatMapConcat(_.body)
      .via(Framing.delimiter(ByteString("\r\n"), 20000))
      .map(bs => Json.parse(bs.utf8String))
      .filter { jsValue =>
        (jsValue \ "event").asOpt[String].forall(_ == "user_update") && (jsValue \ "created_at").asOpt[String].isDefined &&
          (jsValue \ "text").asOpt[String].isDefined && (jsValue \ "truncated").asOpt[JsBoolean].contains(JsBoolean(false))
      }
      .map { jsValue =>
        val tweet = jsValue.as[Tweet]
        Try[String] {
            val pathToScript = Seq("sentiment", "mySentimentAnalysis.py").mkString(File.separator)
            val command = "python3 " + pathToScript + " \"" + tweet.text + "\""
            println(command)
            val res = command.!!.trim
            println(res)
            res
          }
          .map(sentimentClass => (sentimentClass, mapSentimentToLabel(sentimentClass)))
          .map { case (sentimentClass, sentimentLabel) =>
            Json.toJson(tweet).as[JsObject] + ("sentiment", JsString(sentimentLabel)) + ("sentimentClass", JsString(sentimentClass))
          }
          .recoverWith {
            case e: Throwable =>
              logger.error("Exception occured in stream", e)
              Failure(e)
          }
          .getOrElse[JsValue](JsNull)
      }
  }

  private def mapSentimentToLabel(s: String) = s match {
    case "pos" => "Positive"
    case "neg" => "Negative"
    case "neu" => "Neutral"
  }

  def searchTweets() = Action { implicit request =>
    Ok(views.html.searchTweets())
  }

  def searchTweetsNow() = Action { implicit request =>
    Ok(JsObject(Map("text" -> JsString("NEW TWEET TEXT"))))
  }
}