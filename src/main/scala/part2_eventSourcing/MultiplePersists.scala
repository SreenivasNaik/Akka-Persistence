package part2_eventSourcing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor

import java.util.Date

object MultiplePersists extends App {

  /* Diligent accountant : with every invoice, will persist two events
      - Tax record
      -> Invoice Records
  *
  * */

  // COMMAND
  case class Invoice(recipient:String,date:Date,amount:Int)

  // EVENTS
  case class TaxRecord(taxId:String,recordId:Int,date:Date,totalAmount:Int)
  case class InvoiceRecord(invoiceRecordId:Int,recipient:String,date:Date,amount:Int)

  object DiligentAccountant{
    def props(taxId:String,taxAuthority:ActorRef) = Props(new DiligentAccountant(taxId,taxAuthority))
  }
  class DiligentAccountant(taxId:String, taxAuthority:ActorRef) extends PersistentActor with ActorLogging{
    var latestTaxId = 0
    var latestInvoiceId = 0
    override def persistenceId: String = "Diligint Accountant"

    override def receiveCommand: Receive = {
      case Invoice(recipient,date,amount) =>
        // JOURNAL ! TaxRecord
          persist(TaxRecord(taxId,latestTaxId,date,amount/2)){ record =>
              taxAuthority ! record
              latestTaxId += 1
            persist("I here by declare  tax is correct"){ declare =>
              taxAuthority ! declare

            }
          }
        // Journal ! InvoiceRecord
        persist(InvoiceRecord(latestInvoiceId,recipient,date,amount)){ invoiceRecord =>
          taxAuthority ! invoiceRecord
          latestInvoiceId += 1
          persist("I here by declare invoice is correct"){ declare =>
            taxAuthority ! declare

          }
        }
    }

    override def receiveRecover: Receive = {
      case event => log.info(s"received event : $event")
    }
  }
  class TaxAuthority extends Actor with ActorLogging{
    override def receive: Receive = {
      case message => log.info(s"Received $message")
    }
  }

  val system = ActorSystem("Multiple")
  val taxAuthority = system.actorOf(Props[TaxAuthority],"APGovt")
  val accountant  = system.actorOf(DiligentAccountant.props("AP_GOVT_1111",taxAuthority))

  accountant ! Invoice("THe Sreenu",new Date,2000)

  // Message ordering is GAURANTEED
  /*
  *  PERSISTENCE IS ALSO BASED ON THE MESSAGE PASSING=> JOURNALS ASLO IMPLEMENTED USING ACTORS
  * */

  // Nested Persistance

  accountant ! Invoice("THe super Sreenu",new Date,2000)
}
