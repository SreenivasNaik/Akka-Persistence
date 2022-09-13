package part4_practices

import akka.NotUsed
import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.journal.{Tagged, WriteEventAdapter}
import akka.persistence.query.{EventEnvelope, Offset, PersistenceQuery}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.DurationInt
import scala.util.Random



object PersistentQueryDemo extends App {

  val actorSystem = ActorSystem("PersistentActorsDemo",ConfigFactory.load().getConfig("persistentQuery"))

  // Read Journal
  val readJournal = PersistenceQuery(actorSystem).
    readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

    // Give me all Persistence Ids
  val persistenceIds:Source[String,NotUsed] = readJournal.persistenceIds()

  implicit val meterializer = ActorMaterializer()(actorSystem)
//  persistenceIds.runForeach{ persistenceId =>
//    println(s"Found the Persistence id :: $persistenceId")
//  }

  class SimplePersistentQuery extends PersistentActor with ActorLogging{


    override def receiveRecover: Receive  = {
      case m => log.info(s"Recovered ${m}")
    }
    override def receiveCommand: Receive = {
      case m =>
        persist(m) { e=>
          log.info(s"Added $e")

        }
    }



    override def persistenceId: String = "SimpleQueryPersistent"


  }
  val simplePersistentQuery = actorSystem.actorOf(Props[SimplePersistentQuery],"simpleActor")

  import actorSystem.dispatcher
  actorSystem.scheduler.scheduleOnce(5 seconds){
    val message = "HelloNaik"
    simplePersistentQuery ! message
  }

  // PICK EVENTS BY PERSISTENCE IDS

  val events:Source[EventEnvelope,NotUsed] =
    readJournal.eventsByPersistenceId("SimpleQueryPersistent",0,Long.MaxValue)
  events.runForeach{ evnets =>
    println(s"Read Events : $evnets")

  }

  // Events by Tags

  val geners = Array("pop","rock","hip-hop","sdsd","aass")
  case class Song(artist:String,title:String,genre:String)
  case class PlayList(songs:List[Song])

  case class PlayListPurchased(id:Int,songs:List[Song])


  class MusicStoreCheckoutActor extends PersistentActor with ActorLogging{
  var latestPlayListId = 0

    override def receiveRecover: Receive  = {
      case event @PlayListPurchased(id,songs) =>
        log.info(s"Recovered ${event}")
        latestPlayListId = id
    }
    override def receiveCommand: Receive = {
      case PlayList(songs) =>
        persist(PlayListPurchased(latestPlayListId,songs)) { e=>
          log.info(s"user purchsed $e")
        latestPlayListId +=1
        }
    }



    override def persistenceId: String = "MusicStoreCheckoutActor"


  }
  class MusicStoreEventAdaptor extends WriteEventAdapter {
    override def manifest(event: Any): String = "???"

    override def toJournal(event: Any): Any = event match {
      case  event @ PlayListPurchased(_,songs)=>
        val genres = songs.map(_.genre).toSet
        Tagged(event,genres)
    }
  }

  val checkoutActor = actorSystem.actorOf(Props[MusicStoreCheckoutActor],"MusicStoreCheckoutActor")
  val r = new Random
//
//  for(_<- 1 to 10){
//    val maxSongs = r.nextInt(5)
//    val songs = for(i<- 1 to maxSongs) yield {
//      val random = geners(r.nextInt(5))
//      Song(s"Artist $i","My LoveSong", random)
//    }
//    checkoutActor ! PlayList(songs.toList)
//  }

  val rockPlayList = readJournal.eventsByTag("pop",Offset.noOffset)

  rockPlayList.runForeach(playList =>
  println(s"PlayList:: $playList"))

}
