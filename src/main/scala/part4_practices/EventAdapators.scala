package part4_practices

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.journal.{EventSeq, ReadEventAdapter}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable

object EventAdapators extends App {

  /* Store for Guitar
  *
  * */
  val ACCOUSTIC = "acoustic"
  val ELECTRIC = "electric"

  // data structure
  case class Guitar(id:String,model:String,make:String,guitarType:String = ACCOUSTIC)

  case class AddGuitar(guitar: Guitar,quantity:Int)

  // event
  case class GuitarAddedV2(guitarId:String,guitarModel:String,guitarMake:String,quantity:Int,guitarType:String= ACCOUSTIC)
  case class GuitarAdded(guitarId:String,guitarModel:String,guitarMake:String,quantity:Int)

  class InventoryManager extends PersistentActor with ActorLogging{
     val inventory: mutable.Map[Guitar,Int] = new mutable.HashMap[Guitar,Int]()

    override def receiveRecover: Receive = {
      case GuitarAddedV2(id,model,make,quantity,guitarType) =>
        val guitar = Guitar(id,model,make,guitarType)
        addGuitarInventory(guitar, quantity)
        log.info(s"Recovered V2 : $quantity * $guitar to inventory")
    }

    override def receiveCommand: Receive = {
      case AddGuitar(guitar @ Guitar(id,model,make,guitarType),quantity) =>
        persist(GuitarAddedV2(id,model,make,quantity,guitarType)) { e=>
          addGuitarInventory(guitar, quantity)
          log.info(s"Added $quantity * $guitar to inventory")

      }
      case "Print" => log.info(s"current Inventory : $inventory")
    }

    private def addGuitarInventory(guitar: Guitar , quantity: Int) = {
      val existingQuantity = inventory.getOrElse(guitar, 0)
      inventory.put(guitar, existingQuantity + quantity)
    }

    override def persistenceId: String = "InventoryManager"


  }
  class GuitarReadEventAdaptor extends ReadEventAdapter {
    /* Journal --> serializer --> Read event Adaptor --> Actor
    *
    * */
    override def fromJournal(event: Any, manifest: String): EventSeq = event match {
      case GuitarAdded(guitarId,guitarModel,guitarMake,quantity) =>
        EventSeq.single(GuitarAddedV2(guitarId,guitarModel,guitarMake,quantity,ACCOUSTIC))
      case other => EventSeq.single(other)
    }
  }
  val system = ActorSystem("eventAdaptors",ConfigFactory.load().getConfig("eventAdaptor"))
  val inventoryManager = system.actorOf(Props[InventoryManager],"inventoryManagar")
  val guitar = for(i<- 1 to 10) yield Guitar(s"$i",s"Sreenivas-$i",s"Naik")
//
//  for (elem <- guitar) {
//    inventoryManager ! AddGuitar(elem,5)
//  }
}
