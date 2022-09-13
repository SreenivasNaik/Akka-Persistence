package part3_stores_serialization

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object PostgreesSQL extends App {

  val system = ActorSystem("PostgresDemo",ConfigFactory.load().getConfig("PostgresDemo"))
  val persistentActor = system.actorOf(Props[SimplePersistentActor],"SimplePersistentPostgresActor")
  for(i<- 1 to 10)
    persistentActor ! s"I love Akka [$i]"
  persistentActor ! "print"
  persistentActor ! "snap"
  for(i<- 11 to 20)
    persistentActor ! s"I love Akka [$i]"
}
