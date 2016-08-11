package controllers

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

import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

@Singleton
class HomeController @Inject() (val messagesApi: MessagesApi, val ws: WSClient) extends Controller with I18nSupport {

  val logger: Logger = Logger("Stream logger")

  val decider: Supervision.Decider = { e =>
    logger.error("Unhandled exception in stream", e)
    Supervision.Stop
  }
  implicit val system = ActorSystem("reactive-tweets")
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

  var lastSent: JsValue = JsNull

  def index = Action { implicit request =>
    Ok(views.html.index("S"))
  }

  def search = Action {
    Ok.chunked(initRequest() via EventSource.flow).as(ContentTypes.EVENT_STREAM).withHeaders("Content-Encoding" -> "identity")
  }

  def mapLabel(s: String) = s match {
    case "pos" => "Positive"
    case "neg" => "Negative"
    case "neu" => "Neutral"
  }

  private def initRequest() = {
    val response = ws.url("https://stream.twitter.com/1.1/statuses/sample.json")
      .withQueryString("language" -> "en") // TODO: frazy
      .sign(OAuthCalculator(HomeController.key, HomeController.token))
      .withMethod(HttpMethod.GET.name())
      .stream()

    Source.fromFuture(response)
      .delay(5 seconds)
      .flatMapConcat(_.body)
      .via(Framing.delimiter(ByteString("\r\n"), 20000))
      .map { bs =>
        Json.parse(bs.utf8String)
      }
      .filter { jsValue =>
        (jsValue \ "event").asOpt[String].forall(_ == "user_update") && (jsValue \ "created_at").asOpt[String].isDefined &&
          (jsValue \ "text").asOpt[String].isDefined
      }
      .map { jsValue =>
        var result: JsValue = JsNull
        try {
          val tweet = jsValue.as[Tweet]
          val command = "C:\\Users\\ekedz\\Anaconda3\\python.exe C:/Users/ekedz/PycharmProjects/sentiment/twitter/mySentimentAnalysis.py \"" + tweet.text + "\""
          val saResult = command.!!
          val label = mapLabel(saResult.trim())
          result = Json.toJson(tweet).as[JsObject] + ("sentiment", JsString(label))
        } catch {
          case e: Exception => logger.error("ERROR", e);
        }
        result
      }
  }
}

object HomeController {
  val key: ConsumerKey = ConsumerKey("aMs4H5LwxiUDOg1l7w6GTavAS", "kLOlkVXX1zmuYdMUHDsnRXSfG6PZQKjHSJWUgvvUAUUSjvvYXz")
  val token: RequestToken = RequestToken("718512636523081729-AzY9a6MuTBcFbHE61qkA4Jn4RBUg5Nn", "wJKpJEpyP7qZSNTqQzZU3wIhuqWb1KPdrIN1jTUz09rs1")
}