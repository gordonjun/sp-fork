//package sp.system
//
//import akka.actor.{Actor, ActorRef, Props}
//import sp.system.messages._
//
///**
// * Created by Kristofer on 2014-06-18.
// */
//class TestApp extends App {
//
//
//  // temp test
//
//  val testA = system.actorOf(Props(classOf[TestA], modelHandler), "testA")
//
//  private def readFromLineToActor() = {
//    def waitEOF(): Unit = Console.readLine() match {
//      case "exit" => system.shutdown()
//      case s: String => testA ! s; waitEOF()
//      case _ => waitEOF()
//    }
//    waitEOF()
//  }
//
//  readFromLineToActor()
//
//
//}
//
//
//
//class TestA(mh: ActorRef) extends Actor {
//    import sp.domain._
//    var o1: IDAble = Operation("o1")
//    val mid = sp.domain.ID.newID
//    val model = CreateModel(mid, "test")
//    def receive = {
//      case "createM" => mh ! model
//      case "newop" => mh ! UpdateIDs(mid, List(Operation("kalle")))
//      case "updateOp" => mh ! UpdateIDs(mid, List(Operation("hej")))
//      case "op1" => mh ! GetIds(mid, List(o1.id))
//      case "ops" => mh ! GetOperations(mid)
//      case "things" => mh ! GetThings(mid)
//      case "diff" => mh ! GetDiff(mid, 5)
//      case "getIDs" =>
//      case x @ SPIDs(ids) => {
//        println(x)
//        if (ids.exists(_.id == o1.id)) for{o <- ids.find(_.id == o1.id)} yield(o1 = o)
//      }
//      case x @ ModelDiff(mid, ids, del, info, model, prevV, v, t) => println(x)
//      case x: Any => println(x)
//    }
//  }
