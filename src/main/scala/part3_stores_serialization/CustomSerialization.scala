package part3_stores_serialization

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.serialization.Serializer
import com.typesafe.config.ConfigFactory

// Command

case class RegisterUser(email:String,name:String)
// EVENT
case class UserRegistered(id:Int,email:String,name:String)

// SERIALIZATION
class UserRegistrationSerializer extends Serializer{
  val SEPARATOR = "//"
  override def identifier: Int = 1490

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event @ UserRegistered(id,email,name) =>
        println(s"Serializing the event :: $event")
      s"[$id$SEPARATOR$email$SEPARATOR$name]".getBytes()
    case _ => throw new IllegalArgumentException("Only UserRegistration event supported")
  }

  override def includeManifest: Boolean = false

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val string = new String(bytes)
    val values = string.substring(1,string.length -1).split(SEPARATOR)
    val id = values(0).toInt
    val email = values(1)
    val name = values(2)
    val result = UserRegistered(id,email,name)
    println(s"Deserialized $string to $result")
    result

  }
}

class UserRegistrationActor extends PersistentActor with ActorLogging{
  var curentUserId = 0
  override def receiveRecover: Receive = {
    case evnnt @ UserRegistered(id,_,_) => log.info(s"Recovered $evnnt")
      curentUserId = id
  }

  override def receiveCommand: Receive = {
    case RegisterUser(email,name) =>
      persist(UserRegistered(curentUserId,email,name)){e=>
        log.info(s"Persisted ${e}")
        curentUserId +=1
      }
  }

  override def persistenceId: String = "User-register"
}
object CustomSerialization extends App {

   /*
   *  Send a command to the actor
   *    -> actor calls persist
   *    -> serializer serilizes the events into bytes
   *    -> the journals write the bytes
   * */

  val system = ActorSystem("Custom",ConfigFactory.load().getConfig("customSerializtingDemo"))
  val userRegistrationActor = system.actorOf(Props[UserRegistrationActor],"UserRegistrationActor")
  for(i<- 1 to 10)
      userRegistrationActor ! RegisterUser(s"user_$i@sreenu.com",s"User_$i")


}
