package controllers

import play.api.mvc.{Controller, Action}
import play.api.libs.iteratee._
import play.api.libs.json.JsValue
import scala.concurrent.duration._
import play.api.libs.json.Json
import play.api.libs.EventSource
import play.api.libs.json._
import play.api.Logger
import twitter4j._
import twitter4j.{Status => TwitterStatus}
import twitter4j.auth._
import twitter4j.conf._
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext.Implicits.global

class TwitterStreamListener(searchQuery: String, config: Configuration) {
 
  val query = new FilterQuery(0, Array(), Array(searchQuery))
 
  val twitterStream = new TwitterStreamFactory(config).getInstance
 
  def listenAndStream = {
    Logger.info(s"#start listener for $searchQuery")
 
    val (enum, channel) = Concurrent.broadcast[(String, TwitterStatus)]
 
    val statusListener = new StatusListener() {
 
      override def onStatus(status: TwitterStatus) = {      
       Logger.debug(status.getText)  
       channel push (searchQuery, status)
      }
 
      override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) = {}
 
      override def onTrackLimitationNotice(numberOfLimitedStatuses: Int) = {}
 
      override def onException(ex: Exception) = ex.printStackTrace()
 
      override def onScrubGeo(userId: Long, upToStatusId: Long) = {}
 
      override def onStallWarning(warning: StallWarning) = {}
 
    }
 
    twitterStream.addListener(statusListener)
    twitterStream.filter(query)
    enum
  }
 
}
object Application extends Controller { 

  val cb = new ConfigurationBuilder()
  cb.setDebugEnabled(true)
    .setOAuthConsumerKey("xxx")
    .setOAuthConsumerSecret("xxx")
    .setOAuthAccessToken("xxx")
    .setOAuthAccessTokenSecret("xxx")

  val config = cb.build 
 
  val toJson : Enumeratee[(String, TwitterStatus), JsValue] = Enumeratee.map[(String,TwitterStatus)] { case (searchQuery, status) =>
    Json.obj("message" -> s"$searchQuery : ${status.getText}", "author" -> status.getUser.getName)
  }
  
  def stream(query: String) = Action {
    val queries = query.split(",")

    val streams = queries.map { query => 
      val twitterListener = new TwitterStreamListener(query, config)
      twitterListener.listenAndStream
    }

    val mixStreams = streams.reduce((s1,s2) => s1 interleave s2)
    val jsonMixStreams = mixStreams through toJson

    Ok.chunked(jsonMixStreams through EventSource()).as("text/event-stream")  
  } 

  def liveTweets(query: List[String]) = Action {        
    Ok(views.html.index(query))
  }

  def index = Action {
    //default search  
    Redirect(routes.Application.liveTweets(List("java", "ruby")))
  }
  
}
