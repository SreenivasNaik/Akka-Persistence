package part3_stores_serialization

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.typesafe.config.ConfigFactory

object LocalStores extends App {

  val system = ActorSystem("localStoreSystem",ConfigFactory.load().getConfig("localStores"))
  val persistentActor = system.actorOf(Props[SimplePersistentActor],"SimplePersistentActor")
  for(i<- 1 to 10)
    persistentActor ! s"I love Akka [$i]"
  persistentActor ! "print"
  persistentActor ! "snap"
  for(i<- 11 to 20)
    persistentActor ! s"I love Akka [$i]"

}
