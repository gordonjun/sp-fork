package sp.fakeElvis

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._

import sp.fakeElvis.{API_PatientEvent => api}

class FakeElvisDevice extends Actor {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  // activate the extension
  val mediator = DistributedPubSub(context.system).mediator

  def receive = {
    case mess: String => {
      val header = SPHeader(from = "fakeElvisService")
      val body = api.ElvisData(mess)
      val elvisDataSPMessage = FakeElvisComm.makeMess(header, body)
      mediator ! Publish("felvis-data-topic", elvisDataSPMessage)
    }
  }
}

object FakeElvisDevice {
  def props = Props(classOf[FakeElvisDevice])
  implicit val system = ActorSystem("SP")
  val felvisPublisher = system.actorOf(Props[FakeElvisDevice], "felvis-publisher")

  val felvisdataPath = getClass.getResource("/output170201-0217.txt") // ("/output170201-0217.txt") //

  def readFromFile : collection.Iterator[String] = {
    val source = scala.io.Source.fromFile(felvisdataPath.getPath, "utf-8")
    val lines = try source.getLines mkString "\n" finally source.close() // getLines returns an Iterator[String] which mkString turns into a String
    lines.split(",\n").iterator // splits read results to an Array which is made into a collection.Iterator[String]
  }

  while (true) {
    var li = readFromFile // the file is only read once. Probably compiler optimising.
    while (li.hasNext) {
      Thread.sleep(500)
      felvisPublisher ! li.next()
    }
  }
}
