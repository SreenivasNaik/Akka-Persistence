package part2_eventSourcing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

object PersistAsyncDemo extends App {

  case class Command(contents:String)
  case class Events(contents:String)
  object CriticalStreamProcessor{
    def props(eventAggregator: ActorRef) = Props(new CriticalStreamProcessor(eventAggregator))

  }
  class CriticalStreamProcessor(eventAggregator: ActorRef) extends PersistentActor with ActorLogging{
    override def receiveRecover: Receive = {
      case message => log.info(s"recovered $message")
    }

    override def receiveCommand: Receive = {
      case Command(contents) =>
        eventAggregator ! s"Processing $contents"
        persistAsync(Events(contents))/*
        Time Gap => we don't stash if we use persistAsync
        */{
          e=>
          eventAggregator ! e
        }
        val processedContent = contents+"_Processed"
        persistAsync(Events(processedContent)){e=>
          eventAggregator ! e
        }
    }

    override def persistenceId: String = "Critical"
  }
  class EventAggregator extends Actor with ActorLogging{
    override def receive: Receive = {
      case message => log.info(s"Aggregating $message")
    }
  }

  val system = ActorSystem("Ayscm")
  val eventAggregator = system.actorOf(Props[EventAggregator],"eventAggegator")
  val streamProcessor = system.actorOf(CriticalStreamProcessor.props(eventAggregator),"streamProcessor")

  streamProcessor ! Command("command 1")
  streamProcessor ! Command("command 2")

  /* PersistAsync -> high throughput b
    =>
  *
  * */
}
