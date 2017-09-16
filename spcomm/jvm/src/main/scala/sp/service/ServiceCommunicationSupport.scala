package sp.service

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator._
import sp.domain._

trait ServiceCommunicationSupport {
  val context: ActorContext
  var shComm: Option[ActorRef] = None
  def triggerServiceRequestComm(mediator: ActorRef, resp: APISP.StatusResponse): Unit = {
    val x = context.actorOf(Props(classOf[ServiceHandlerComm], mediator, resp))
    shComm = Some(x)
  }
  def updateServiceRequest(resp: APISP.StatusResponse): Unit = {
    shComm.foreach(_ ! resp)
  }



}

class ServiceHandlerComm(mediator: ActorRef, resp: APISP.StatusResponse) extends Actor {
  var serviceResponse: APISP.StatusResponse = resp
  mediator ! Subscribe(APISP.serviceStatusRequest, self)
  sendEvent(SPHeader(from = serviceResponse.instanceName, to = APIServiceHandler.service))

  override def receive: Receive = {
    case x: APISP.StatusResponse if sender() != self => serviceResponse = x
    case x: String if sender() != self =>
      for {
        mess <- SPMessage.fromJson(x)
        h <- mess.getHeaderAs[SPHeader]
        b <- mess.getBodyAs[APISP] if b == APISP.StatusRequest
      } yield {
        sendEvent(h.copy(to = h.from, from = serviceResponse.instanceName))
      }
  }

  override def postStop() = {
    println("MODEL ServiceComm handler removed: " + resp.instanceID)
    super.postStop()
  }

  def sendEvent(h: SPHeader) =
    mediator ! Publish(APISP.serviceStatusResponse, SPMessage.makeJson(h, serviceResponse))
}
