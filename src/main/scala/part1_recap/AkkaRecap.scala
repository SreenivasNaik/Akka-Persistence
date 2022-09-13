package part1_recap

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorSystem, OneForOneStrategy, Props, Stash, SupervisorStrategy}
import akka.util.Timeout


object AkkaRecap extends App {

  class SimpleActor extends Actor with Stash{
    override def receive: Receive = {
      case "stash" =>
        stash()
      case message => println("s")
      case "change" =>
        unstashAll()
        context.become(becomeNew)
    }
    def becomeNew:Receive = {
      case meessge => println("Hh")
    }

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(){
      case _:RuntimeException => Restart
      case _ => Stop
    }
  }
  // actor encapsulation => we can't create actor instance directly instead we have to create the Props and pass
  val system = ActorSystem("System")
  // #1 . You can only instantiate an actor wuing through actor system
  val simpleActor = system.actorOf(Props[SimpleActor],"simpleActor")
 // #2 . Only way to communicate with actors are by sending message => tell (!) , ask (?)
    simpleActor ! "Hello"
  /*  Messages are sent in Asynchronously
  * many actors can share the dozens of threads
  *  each message is processed / handled ATOMCALLY
  *  NO NEED of Locking
  * */

  /*
  * we can change the behaviour of actor by context.become and we can stash the messages using Stash Trait
  *
  * Actor can create child actors [
  *  Gaurdians => /system /user / root
  *
  * Life Cycle
  *
  * Stoping => context.stop
  * PoisonPill handled in separate mail box
  *
  * logging => ActorLogging
  *
  * Supervison =>
  *
  * // Configuration = dispatcher , routers , MailBoxes
  *
  * // Scheduler
  * */

  import scala.concurrent.duration._
  import system.dispatcher
  system.scheduler.scheduleOnce(2 seconds){
    simpleActor ! "Hello"
  }
  // Akka Patterns FSM , Ask

  import akka.pattern.ask
  implicit val timeout = Timeout(2 seconds)
  val future = simpleActor ? "Hello"

  import akka.pattern.pipe
  val anotherActor = system.actorOf(Props[SimpleActor],"AnotherActor")
  future.mapTo[String].pipeTo(anotherActor)
}
