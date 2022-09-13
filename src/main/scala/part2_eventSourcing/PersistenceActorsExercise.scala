package part2_eventSourcing

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

import scala.collection.mutable

object PersistenceActorsExercise extends App {

  /* Persistence Actor for a Voting Station
      keep :
        -> the citizens who voted
        - the poll: Mapping between a condidate and the number of received votes so far

     The Actor must be able to recover it's state if it is shutdown or restarted
  *
  * */
  case class Vote(citizenPid:String,candidate:String)


  class VotingStationActor extends PersistentActor with ActorLogging {
    val citizens:mutable.Set[String] = new mutable.HashSet[String]()
    val poll: mutable.Map[String,Int] = new mutable.HashMap[String,Int]()
    override def receiveRecover: Receive = {
case vote @ Vote(citizenPid,candidate) =>
  log.info(s"Recovered $vote")
  handleInternalStateChange(citizenPid,candidate)
    }

    override def receiveCommand: Receive = {
      case vote @ Vote(citizenPid,candidate) => {
        /* 1 . create a Event
          2. Persit the event
          3. Handle the state change
        * */
        if(!citizens.contains(vote.citizenPid)) {
          persist(vote) { _ => // COMMAND SOURCING
            log.info(s"Persisted $vote")
            handleInternalStateChange(citizenPid,candidate)
          }
        }else{
          log.warning(s"Citizen : $citizenPid is trying to vote again")
        }
      }
      case "print" => log.info(s"current State citizens:$citizens \n Votes::${poll}")
    }
    def handleInternalStateChange(citizenPid:String,candidate:String): Unit ={
        citizens.add(citizenPid)
        val votes = poll.getOrElse(candidate,0)
        poll.put(candidate,votes+1)
    }

    override def persistenceId: String = "Simple-VotingStation"
  }

  val system = ActorSystem("PersistenceActorsExercise")
  val votingStationActor = system.actorOf(Props[VotingStationActor],"VotingSystem")
  val votes = Map[String,String] (
    "Abac" -> "marting",
            "Bob" ->  "Roland",
            "chale"-> "marting",
            "David" -> "Jans"
  )
  /*votes.keys.foreach(citizen=>
    votingStationActor ! Vote(citizen,votes(citizen))
  )*/
  votingStationActor ! Vote("David","ANC")
  votingStationActor ! "print"

}
