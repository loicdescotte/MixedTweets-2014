package controllers

import play.api.mvc._
import play.api.libs.oauth._
import play.api.libs.ws.WS
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import play.api.libs.iteratee._
import play.api.libs.json.JsValue
import scala.concurrent.duration._
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.Json
import play.api.Logger
import twitter4j._
import twitter4j.auth._
import twitter4j.conf._
import org.joda.time.DateTime
import play.api.libs.EventSource

class TwitterStreamListener(searchQuery: String, config: Configuration) {
 
  val query = new FilterQuery(0, Array(), Array(this.searchQuery))
 
  val twitterStream = new TwitterStreamFactory(config).getInstance
 
  def listenAndStream = {
    Logger.info(s"#start listener for $searchQuery")
 
    val (enum, channel) = Concurrent.broadcast[String]
 
    val statusListener = new StatusListener() {
 
      override def onStatus(status: Status) = {      
       Logger.debug(status.getText)  
       channel.push(s"$searchQuery : ${status.getText} - ${status.getUser.getName}" )
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
 
	val upperCase = Enumeratee.map[String] {
	  tweet => tweet.toUpperCase
	}
	
	def stream(query: String) = Action {
    val queries = query.split(",")

    val streams = queries.map { query => 
       val twitterListener = new TwitterStreamListener(query, config)
       twitterListener.listenAndStream
    }

    val mixStreams = streams.reduce((s1,s2) => s1 interleave s2)

	  Ok.chunked(mixStreams through upperCase through EventSource()).as("text/event-stream")
  
	}	

	def liveTweets(query: List[String]) = Action {			  
	  Ok(views.html.index(query))
	}

	def index = Action {
	  //default search	
	  Redirect(routes.Application.liveTweets(List("java", "ruby")))
	}
  
}