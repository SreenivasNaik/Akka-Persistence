package part2_eventSourcing

import akka.actor.{ActorLogging, ActorSystem, PoisonPill, Props}
import akka.persistence.PersistentActor

import java.util.Date

object PersistanceActors extends App {

  /*
  *   Scenario: We have a business and an accountant which keeps track of our invoices
  * */
  // COMMAND
  case class Invoice(recipient:String,date:Date,amount:Int)
  case class InvoiceBulk(invoice: List[Invoice])
  case object Shutdown

  // EVENT
   case class InvoiceRecorded(id:Int,recipient:String,date:Date,amount:Int)

  class Accountant extends PersistentActor with ActorLogging{
    var latestInvoiceId = 0
    var totalAmount = 0
    override def persistenceId: String = "Simple-Account" // Make it unique

    // Normal receive method
    override def receiveCommand: Receive = {
      case Invoice(recipient,date,amount) =>
        /* When we get the command
            1. You create an EVENT to persist into the store
            2. You persist te event, the pass in a call back that will get triggered once the Event is Written
            3. update the actor state when the event has persisted
        * */
        log.info(s"Received Invoice for Amount $Invoice")
        persist(InvoiceRecorded(id = latestInvoiceId, recipient = recipient, date = date, amount = amount))
        /* TIme GAP:: ALL OTHER MESSAGES SENT TO ACTOR WILL BE STASHED*/
        {
              // Update state ==> SAFE to update mutable states
          e => latestInvoiceId += 1
            totalAmount += amount
            // wE can correctly identify the sender of the command
       //     sender() ! "PersistanceACK"
            log.info(s"Persisted $e as Invoice #${e.id}, for the total amount $totalAmount")
        }
        // act like nrormal actor
      case "print" => log.info("")
      case InvoiceBulk(invoices) => {
        /* Create the events
          persist all the events
          update the state
        *
        * */

        val invoiceIds = latestInvoiceId to (latestInvoiceId + invoices.size)
        val events = invoices.zip(invoiceIds).map{
          pair =>
            val id = pair._2
            val invoice = pair._1
            InvoiceRecorded(id,invoice.recipient,invoice.date,invoice.amount)
        }
        persistAll(events){
          e => latestInvoiceId += 1
            totalAmount += e.amount
            log.info(s"Persisted SINGLE $e as Invoice #${e.id}, for the total amount $totalAmount")

        }
      }
      case Shutdown => context.stop(self)
    }

    /* Handler that will be called on recovery

    * */
    override def receiveRecover: Receive = {
      /* Best Practice :: Folow the logic in the persist steps of receiveCommand
      *
      * */
      case InvoiceRecorded(id,_,_,amount) =>
        latestInvoiceId = id
        totalAmount += amount
        log.info(s"Recovered Invoice #$id for amount $amount and totalAmount : $totalAmount")
    }

    /*
        This Method will be called when the persistence is failed
        actor will be STOPPED regardless of supervision strategy

        BEST PRACTICE:  Start the actor after  while
        USE BackOff Supervision
    * */
    override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"Failed to persist $event because of $cause")
      super.onPersistFailure(cause,event,seqNr)
    }

    /*
    *  Called when the JOURNAL fails to persist the event
    *
    * The actor will RESUMED
    * */
    override def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
      log.error(s"PERSIST rejected for  $event because of $cause")
      super.onPersistRejected(cause,event,seqNr)
    }
  }

  val system = ActorSystem("PersistanceActorsystem")
  val accountant = system.actorOf(Props[Accountant],"AccountPersistentActor")

  for(i<- 1 to 10){
    accountant ! Invoice("The Sofa company", new Date, i * 1000)
  }

  /*
  *  Persistence Failures
  * */

  /* Persisting Multiple Events
  *  persistAll
  * */

  val newInvoices = for(i<- 1 to 5) yield Invoice("The chair",new Date,i*2000)
 // accountant ! InvoiceBulk(newInvoices.toList)

  /*
  NEVER EVER CALL PERSIST OR PERSISTALL FROM THE FUTURES
  it will break the encapsulation
  * */

  /*
  * Shutdown of persistance actors
  *  Define our Own shutdown messages
  * */

 // accountant ! PoisonPill
   accountant ! Shutdown

}
