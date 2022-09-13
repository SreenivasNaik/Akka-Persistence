package part4_practices

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.persistence.journal.{EventAdapter, EventSeq}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable

object DetachingModels extends App {



  class CouponManager extends PersistentActor with ActorLogging{
    import DomainModel._;
    val coupons:mutable.Map[String,User] = new mutable.HashMap[String,User]()

    override def persistenceId: String = "CoupnManager"

    override def receiveRecover: Receive = {
      case event @ CouponApplied(code,user)=>
        log.info(s"Recovered $event")
        coupons.put(code,user)
    }

    override def receiveCommand: Receive = {
      case ApplyCoupon(coupon,user) =>
        if(!coupons.contains(coupon.code)){
        persist(CouponApplied(coupon.code,user)){ e=>
        log.info(s"Persisted $e")
        coupons.put(coupon.code,user)
      }
        }
    }
  }

  import DomainModel._
  val system = ActorSystem("DetachingModels", ConfigFactory.load().getConfig("detachingModels"))
  val couponManager = system.actorOf(Props[CouponManager], "couponManager")

//    for (i <- 10 to 15) {
//      val coupon = Coupon(s"MEGA_COUPON_$i", 100)
//      val user = User(s"user_$i@rtjvm.com", s"John Doe $i")
//
//      couponManager ! ApplyCoupon(coupon, user)
//    }



}
object DomainModel {
  case class User(id:String,email:String)
  case class Coupon(code:String,promotionAmount:Int)

  // Command
  case class ApplyCoupon(coupon: Coupon,user: User)
  //event
  case class CouponApplied(code:String,user: User)
}

object DataModel{
  case class WrittenCouponApplied(code:String,userId:String,userEmail:String)

}

class ModelAdaptor extends EventAdapter{
  import DataModel._;
  import DomainModel._
  override def manifest(event: Any): String = "CMA"

  // Actor => toJournal => Serializer => journal
  override def toJournal(event: Any): Any = event match {
    case events @ CouponApplied(code,user)=>
      println(s"Converting $events to DataModel")
      WrittenCouponApplied(code,user.id,user.email)
  }

  // Journal => serializer => fromJournal => toTheActor
  override def fromJournal(event: Any, manifest: String): EventSeq = event match {
    case events @ WrittenCouponApplied(code,userId,userEmail) =>
      println(s"converting $events to Domain Model")
      EventSeq.single(CouponApplied(code,User(userId,userEmail)))
    case other => EventSeq.single(other)
  }
}