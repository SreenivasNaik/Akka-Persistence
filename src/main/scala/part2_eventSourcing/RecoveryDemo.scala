package part2_eventSourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, Recovery, RecoveryCompleted, SnapshotSelectionCriteria}

object RecoveryDemo extends App {

  case class Command(contents:String)
  case class Event(id:Int,contents:String)
  class RecoveryActor extends PersistentActor with ActorLogging {

    override def receiveRecover: Receive = {
      case Event(id, contents) =>
        log.info(s"Recovered $contents Recovery is ${if (this.recoveryFinished) "" else "NOT"} finished")
      //        if(contents.contains("314")) {
      //            throw new RuntimeException("I can't handle")
      //        }
      case RecoveryCompleted => log.info("Finished Recovery")
    }

    override def receiveCommand: Receive = online(0)


    override def onRecoveryFailure(cause: Throwable, event: Option[Any]): Unit = {
      log.info(s"I failed to recover at $event and because ${cause.getCause}")
      super.onRecoveryFailure(cause, event)
    }

    override def persistenceId: String = "RecoveryActor"

    def online(latestPersistenceId: Int): Receive = {
      case Command(contents) => persist(Event(latestPersistenceId, contents)) { e =>
        log.info(s"Persisted Event ${e} , Recovery is ${if (this.recoveryFinished) "" else "NOT"} finished")
        context.become(online(latestPersistenceId + 1))
      }
      //  override def recovery: Recovery = Recovery(toSequenceNr = 100)
      //override def recovery: Recovery = Recovery(fromSnapshot = SnapshotSelectionCriteria.Latest)
      // override def recovery: Recovery = Recovery.none

    }
  }
  //Stashing the commands

  val system = ActorSystem("RecoveryDemo")
  val recoveryActor = system.actorOf(Props[RecoveryActor],"recoveryActor")

  for (i<- 1 to 1000)
      recoveryActor ! Command(s"command $i")

  // ALL THE COMMANDS SENT DURING RECOVERY ARE STASHED

  /* FAILURE DURING RECOVERY
      -> it will call onREcoveryFailed method and Actor will be STOPPED
  * */

  /* 3. Customize the recovery
  override def recovery: Recovery = Recovery(toSequenceNr = 100)
      DoNot persist more events afeter this
  *
  * */

  /*
  *   Recovery Status
  * */

  // Stateless Actors
}
