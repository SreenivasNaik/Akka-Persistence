package part3_stores_serialization

import akka.actor.ActorLogging
import akka.persistence.{PersistentActor, RecoveryCompleted, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}


class SimplePersistentActor extends PersistentActor with ActorLogging{
    var nMessages = 0

    override def receiveRecover: Receive = {
      case RecoveryCompleted => log.info("Recovery completed")
      case SnapshotOffer(metadata,payload:Int) => {
        log.info(s"recovered snapshot $payload")
        nMessages = payload
      }
      case message =>
        log.info(s"Recovered $message")
        nMessages += 1
    }

    override def receiveCommand: Receive = {
      case "print" => log.info(s"I have persisted $nMessages so far")
      case "snap" => saveSnapshot(nMessages)
      case SaveSnapshotSuccess(metadata) => log.info(s"Save Snapshot sucessfull metadata: $metadata")
      case SaveSnapshotFailure(_,cause) => log.info(s"Save Snapshot failed with $cause")
      case message => persist(message){e=> log.info(s"I have persisted ${e}")
        nMessages +=1
      }
    }

    override def persistenceId: String = "SimplePersistentAcor"
  }
