package sp.system

import java.lang.Exception

import akka.actor._
import org.json4s.{JNothing}
import sp.domain._
import sp.system.messages._
import sp.domain.Logic._
import akka.pattern.ask
import akka.util.Timeout
import scala.util._
import scala.concurrent.duration._

class ServiceTalker(service: ActorRef,
                    replyTo: ActorRef,
                    serviceAttributes: SPAttributes,
                    request: Request) extends Actor {

  import context.dispatcher
  implicit val timeout = Timeout(2 seconds)
  val cancelTimeout =  context.system.scheduler.scheduleOnce(3 seconds, self, "timeout")

  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{ Put, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator

  val x = request.attributes
  val handleAttr = x.getAs[ServiceHandlerAttributes]("core").getOrElse(
    ServiceHandlerAttributes(None, false, false, List()))

  def receive = {
    case req @ Request(_, attr, ids, _) => {
      if (handleAttr.model.isDefined && ids.isEmpty) {
        mediator ? Publish("modelHandler", GetIds(handleAttr.model.get, List())) onComplete {
          case Success(result) => result match {
            case SPIDs(xs) => {
              val filter = xs.filter(item => handleAttr.includeIDAbles.contains(item.id))
              val res = if (filter.nonEmpty) filter else xs
              service ! req.copy(ids = res)
            }
            case x => {
              replyTo ! x
              killMe
            }
          }
          case Failure(failure) => {
            replyTo ! SPError(failure.getMessage)
            killMe
          }
        }
      } else {
        service ! req
      }
    }
    case "timeout" => {
      replyTo ! SPError(s"Service ${request.service} (actor: $service) is not responding.")
      mediator ! Publish("eventHandler", SPError(s"Service ${request.service} (actor: $service) is not responding."))
      killMe
    }
    case r @ Response(ids, attr, _, _) => {
      if (handleAttr.responseToModel && handleAttr.model.isDefined) {
        mediator ! Publish("modelHandler", UpdateIDs(handleAttr.model.get, ids, attr))
      }
      replyTo ! r
      if (!handleAttr.onlyResponse) {
        mediator ! Publish("eventHandler", r)
      }
      killMe
    }
    case r: Progress => {
      cancelTimeout.cancel()
      if (!handleAttr.onlyResponse) {
        replyTo ! r
        mediator ! Publish("eventHandler",  r)
      }
    }
    case e: SPError => {
      replyTo ! e
      if (!handleAttr.onlyResponse) {
        mediator ! Publish("eventHandler",  ServiceError(request.service, request.reqID, e))
      }
      killMe
    }
    case x if sender() == service => {
      println("SERVICES SHOULD SEND case class Response, NOT "+x)
      replyTo ! x
      killMe
    }
  }

  def killMe = {
    cancelTimeout.cancel()
    self ! PoisonPill
  }

}

object ServiceTalker {
  def props(service: ActorRef,
            replyTo: ActorRef,
            serviceAttributes: SPAttributes,
            request: Request) =
    Props(classOf[ServiceTalker], service, replyTo, serviceAttributes, request)


  def serviceHandlerAttributes = SPAttributes("core" -> SPAttributes(
    "model"-> KeyDefinition("Option[ID]", List(), Some(JNothing)),
    "responseToModel"->KeyDefinition("Boolean", List(true, false), Some(true)),
    "includeIDAbles"->KeyDefinition("List[ID]", List(), Some(SPValue(List[IDAble]()))),
    "onlyResponse"->KeyDefinition("Boolean", List(true, false), Some(false))
  ))
  def transformServiceHandlerAttributes = List[TransformValue[_]](
    TransformValue("core", _.getAs[ServiceHandlerAttributes]("core"))
  )

  def validateRequest(req: Request, serviceAttributes: SPAttributes, transform: List[TransformValue[_]])= {
    val attr = req.attributes
    //println(s"ServceTalker got: $attr")
    val expectAttrs = serviceAttributes.findObjectsWithKeysAs[KeyDefinition](List("ofType", "domain"))
    val errorsAttr = analyseAttr(attr, expectAttrs)
    if (errorsAttr.nonEmpty) Left(errorsAttr) else {
      try {
        val upd = reqUpdate(attr, serviceAttributes)
        val transformers: List[TransformValue[_]] =  transform //++ transformServiceHandlerAttributes
        val tError = analyseTransform(upd, transformers)
        if (tError.nonEmpty) Left(tError) else {
          Right(req.copy(attributes = upd))
        }
      } catch {
        case e: Exception => {
          Left(List(SPError(e.getMessage)))
        }
      }
    }
  }

  import org.json4s._
  private def reqUpdate(reqAttr: SPAttributes, specAttr: SPAttributes): SPAttributes = {
    val sKeys = specAttr.obj.map(_._1)
    sKeys match {
      case Nil => reqAttr
      case x :: xs => {
        val reqObj = reqAttr.get(x)
        val specObj = specAttr.getAs[KeyDefinition](x)
        val updReq: SPAttributes = (specObj, reqObj) match {
          case (Some(KeyDefinition(ofType, domain, default)), Some(value)) => {
            if (domain.nonEmpty && !domain.contains(value)){throw new Exception(s"value: $value is not include in the domain: $domain for key: $x")}
            reqAttr
          }
          case (Some(KeyDefinition(ofType, domain, Some(defaultValue))), None) => {
            reqAttr + (x -> defaultValue)
          }
          case (Some(KeyDefinition(ofType, domain, None)), None) => {
            if (!ofType.toLowerCase.contains("option"))
              throw new Exception(s"Required key test2: $x is missing")
            else
              reqAttr
          }
          case (None, dummy) => {
            (specAttr.get(x), reqObj) match {
              case (Some(value: JObject), Some(req: JObject)) => {
                val fixedBelow = reqUpdate(req, value)
                SPAttributes(reqAttr.obj.filterNot(_._1 == x) :+ (x->fixedBelow))
              }
              case (Some(value: JObject), None) => {
                val fixedBelow = reqUpdate(SPAttributes(), value)
                reqAttr + (x -> fixedBelow)
              }
              case (Some(value: JObject), Some(req: JValue)) => {
                throw new Exception(s"The key: $x expects an object, not: $req")
              }
              case (s,r) => {
                reqAttr
              }
            }
          }
        }
        val updSpec = SPAttributes(specAttr.obj.filterNot(_._1 == x))
        reqUpdate(updReq, updSpec)
      }
    }
  }





  private def analyseAttr(attr: SPAttributes, expected: List[(String, KeyDefinition)]): List[SPError] = {
    expected.flatMap{ case (key, v) =>
      val flatAttr = attr.filterField{x => true}.toMap
      flatAttr.get(key).orElse(v.default) match {
        case None => {
          if (!v.ofType.toLowerCase.contains("option"))
            List(SPError(s"Required key $key is missing"))
          else
            List()
        }
        case x => List()
      }
    }
  }

  private def fillDefaults(attr: SPAttributes, expected: List[(String, KeyDefinition)]): SPAttributes = {
    val flatAttr = attr.filterField{x => true}.toMap
    val d = expected.filter(kv => !flatAttr.contains(kv._1)).map(kv => kv._1 -> kv._2.default.getOrElse(JNothing))
    attr + SPAttributes(d)
  }

  private def analyseTransform(attr: SPAttributes, tr: List[TransformValue[_]]) = {
    tr.flatMap{t =>
      t.transform(attr) match {
        case Some(x) => None
        case None => Some(SPError(s"Couldn't transform the key: ${t.key}"))
      }
    }
  }
}

