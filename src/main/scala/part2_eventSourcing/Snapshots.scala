package part2_eventSourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}

import scala.collection.mutable

object Snapshots extends App {

  // COMMANDS
  case class ReceiveMessage(contents:String) // messages from the contacts
  case class SentMessage(contents:String) // messages from the you

  // Events
  case class ReceivedMessageRecord(id:Int,contents:String)
  case class SentMessageRecord(id:Int,contents:String)

  object Chat{
    def props(owner:String,contact:String)=Props(new Chat(owner,contact))
  }
  class Chat(owner:String,contact:String) extends PersistentActor with ActorLogging{
    val MAX_Message = 10
    var currentMessageId = 0
    var commandsWithoutCheckpoint = 0
    val lastMessages = new mutable.Queue[(String,String)]()
    override def receiveRecover: Receive = {
      case ReceivedMessageRecord(id,contents) =>
        log.info(s"Recovered receivedMessage with id:$id and contents: $contents")
        mayBeReplaceMessage(contact,contents)
        currentMessageId = id
      case SentMessageRecord(id,contents) =>
        log.info(s"Recovered SentMessage with id:$id and contents: $contents")
        mayBeReplaceMessage(owner,contents)
        currentMessageId = id
      case SnapshotOffer(metadata,contents) =>
        log.info(s"Recovered Snapshot $metadata")
        contents.asInstanceOf[mutable.Queue[(String,String)]].foreach(lastMessages.enqueue(_))
    }

    override def receiveCommand: Receive = {
      case ReceiveMessage(contents) =>
        persist(ReceivedMessageRecord(currentMessageId,contents)) { e=>
          log.info(s"Received Message : $contents")
        mayBeReplaceMessage(contact,contents)
          currentMessageId += 1
          maybeCheckPoint()
      }
      case SentMessage(contents) => {
        persist(SentMessageRecord(currentMessageId,contents)){e =>
          log.info(s"Sent Message ${e}")
          mayBeReplaceMessage(owner,contents)
          currentMessageId += 1
        }
      }
      case "print" =>
        log.info(s"Most recent message:: $lastMessages")
      case SaveSnapshotSuccess(metadata) =>
        log.info(s"saving the snapshot sucess $metadata")
      case SaveSnapshotFailure(metadata,reason) => log.warning(s"saving the snapshot failed $metadata with reason $reason")
    }

    override def persistenceId: String = s"$owner-$contact-chat"

    def mayBeReplaceMessage(sender:String,contents:String): Unit ={
      if(lastMessages.size >= MAX_Message){
        lastMessages.dequeue()
      }
      lastMessages.enqueue((sender,contents))
    }
    def maybeCheckPoint():Unit = {
      commandsWithoutCheckpoint += 1
      if(commandsWithoutCheckpoint >= MAX_Message){
        log.info(s"Saving CheckPoint")
        saveSnapshot(lastMessages) // Asynchronously
        commandsWithoutCheckpoint = 0
      }
    }

  }
  val system = ActorSystem("ChatDemo")
  val chat = system.actorOf(Chat.props("Sreenu","Jyothi"))

//  for(i<- 1 to 100000){
//    chat ! ReceiveMessage(s"Hello Sreenu $i")
//    chat ! SentMessage(s"Hello Jyothi $i")
//  }
  chat ! "print"

  /*
  * After each persist, may be save a snapshot
  * if yoy save the snapshot handle SnapshotOffer message in receiveRecover
  * handle SaveSnapshotSuccess,SaveSnapshotFailure in receiveCommand
  * */
}
