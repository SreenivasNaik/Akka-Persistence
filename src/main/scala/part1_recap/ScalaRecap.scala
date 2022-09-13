package part1_recap

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ScalaRecap extends App {
  val acondition:Boolean = false

  def myDunction(x:Int) = {
    //
    if(x>42) 43 else 65
  }
  // Types + Type Inference

  // OO Features of scala

  class Animal

  trait  Carnivore{
    def eat(e:Animal):Unit
  }
  object Carnivore

  // Generics
  abstract class MyList[+A]

  // Method Notations
   1 + 2 // Infix notation
  1.+(2)

  // FP

  val anIncremental:Int=>Int = (x:Int) => x+1

  List(1,2,3).map(anIncremental)

  // HOP - flatmap, map ,filter
  // FOr comprehensions

  // Monads : Option, Try

  // Pattern Matching

  try {

  }catch {
    case e:Exception => println("sdsd")
  }

  /* Scala Advanced
  * */
  // MultiThreading

  import  scala.concurrent.ExecutionContext.Implicits.global
  val future = Future{
    //
    23
  }
  // Map, flatmap,filter / recover,recverwith
  future.onComplete{
    case Success(value) => println("sdsd")
    case Failure(exception) => println("exceoption")
  } // ON some thread

  // Partial Functions

  val partialFunction:PartialFunction[Int,Int] = {
    case 1 => 23
    case 2 => 34
    case _ => 2323
  } // Based on the pattern matching

  // type aliases
  type AkkaReceive = PartialFunction[Int,Unit]
  def receive:AkkaReceive = {
    case 1 => println("Hello")
    case _ => println("Confused")
  }

  // Implicits
  implicit val timeout = 3000
  def setTimeout(f:()=>Unit)(implicit timeout:Int) = f(

  )
  setTimeout(()=>println("Hello"))
  // conversions
  // 1, Implicit methods
  case class Person(name:String) {
    def greet:String = s"Hi My name $name"
  }
  implicit def fromStringToPerson(name:String) = Person(name)

  "peter".greet // fromStringToPerson("Peter").greet

  // Implicit classes
  implicit class Dog(name:String) {
    def bark= println("bark")
  }
  "Lassi".bark // new Dog("Lassi).bark

  // Implict organizations
  // Local scope
  // Imported scope
  // Companion objects of the types invloved in the call
}
