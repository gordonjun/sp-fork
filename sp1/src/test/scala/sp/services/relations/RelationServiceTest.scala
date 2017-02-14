package sp.services.relations

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.testkit.TestKit
import org.scalatest._
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import akka.testkit.ImplicitSender
import scala.concurrent.duration._
import com.typesafe.config._
import sp.domain._
import sp.services.specificationconverters.ConditionsFromSpecs
import sp.system.messages._

/**
 * Created by Kristofer on 2014-06-17.
 */
//class RelationServiceTest extends TestKit(ActorSystem("test")) with FreeSpecLike with ImplicitSender
//  with Matchers with BeforeAndAfterAll {
//
//  import TestThings._
//
//  override def afterAll {
//    TestKit.shutdownActorSystem(system)
//  }
//
//  import Attribs._
//
////  "The relation service" - {
////    val mid = ID.newID
////
////    "find rels simple" in {
////      val mh = system.actorOf(Props(classOf[TestModel], List(o1,o2), ModelInfo(mid, "test", 1, Attr()), List(v1,v2,v3), List(), false))
////      val sh = mh
////      val rls = system.actorOf(RelationService.props(mh, sh, "condService"))
////      val attr = Attr("model"->"test", "operations"->ListPrimitive(List(o1.id, o2.id))).addStateAttr("initstate", state)
////      rls ! Request("RelationFinder", attr)
////      expectMsgPF(1 second){case RelationResult(name, rels, model, mversion, attr, id) => true}
////    }
////    "find rels unsing SOPSpecs" in {
////      val sop = Sequence(o3, o4)
////      val spec = SOPSpec("spec", List(sop))
////      val mh = system.actorOf(Props(classOf[TestModel], List(o3,o4), ModelInfo(mid, "test", 1, Attr()), List(v1,v2,v3), List(spec), false))
////      val sh = mh
////      val rls = system.actorOf(RelationService.props(mh, sh, "condService"))
////      val attr = Attr("model"->"test", "operations"->ListPrimitive(List(o3.id, o4.id))).addStateAttr("initstate", state)
////      rls ! Request("RelationFinder", attr)
////      expectMsgPF(1 second){
////        case RelationResult(name, rels, model, mversion, attr, id) if rels.relations(Set(o3.id,o4.id)).isInstanceOf[Sequence] =>
////          println("relations between o3 - o4: " + rels.relations(Set(o3.id, o4.id)))
////      }
////    }
////  }
//}
//
//object TestThings {
//  val range = MapPrimitive(Map("start" -> SPAttributeValue(0), "end" -> SPAttributeValue(3), "step" -> SPAttributeValue(1)))
//  val domain = ListPrimitive(List(StringPrimitive("hej"), StringPrimitive("då")))
//  val attrD = SPAttributes(Map("domain" -> domain))
//  val attrR = SPAttributes(Map("range" -> range))
//  val attrB = SPAttributes(Map("boolean" -> true))
//
//  import sp.domain.logic.StateVariableLogic._
//  import sp.domain.logic.OperationLogic._
//
//  val sv1 = StateVarInfo(DomainList(List("hej", "då")))
//  val sv2 = StateVarInfo(DomainRange(new Range(0, 3, 1)))
//  val sv3 = StateVarInfo(DomainBool)
//
//  val v1 = Thing("v1").addStateVar(sv2)
//  val v2 = Thing("v2").addStateVar(sv1)
//  val v3 = Thing("v2").addStateVar(sv3)
//
//
//  val eq = EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(0)))
//  val neq = NEQ(SVIDEval(v2.id), ValueHolder(SPAttributeValue("då")))
//
//  val o1Cond = PropositionCondition(
//    EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(0))),
//    List(Action(v1.id, ValueHolder(1))))
//  val o2Cond = PropositionCondition(
//    EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(1))),
//    List(Action(v1.id, ValueHolder(2))))
//  val noActionCond = PropositionCondition(
//    EQ(SVIDEval(v1.id), ValueHolder(SPAttributeValue(0))),
//    List())
//
//  val o1 = Operation("o1", List(o1Cond))
//  val o2 = Operation("o2", List(o2Cond))
//  val o3 = Operation("o3")
//  val o4 = Operation("o4")
//
//  val state = State(Map(v1.id -> 0, v2.id -> "hej", v3.id -> false, o1.id -> "i", o2.id -> "i"))
//  val state2 = State(Map(v1.id -> 2, v2.id -> "då", v3.id -> false, o1.id -> "i", o2.id -> "i"))
//
//  val vm=Map(v1.id -> v1.inDomain, v2.id -> v2.inDomain, v3.id -> v3.inDomain, o1.id -> o1.inDomain, o2.id -> o2.inDomain)
//
//}
//
//
//class TestModel(ops: List[Operation],
//                modelInfo: ModelInfo,
//                specs: List[SOPSpec],
//                fail: Boolean) extends Actor {
//
//  import TestThings._
//
//  def receive = {
//    case x: GetIds => sender ! SPIDs(ops)
//    case x: GetModelInfo => sender ! modelInfo
//    case GetResults(_, f) => sender ! SPIDs(specs filter f)
//    case x: Request => sender ! ConditionsFromSpecs(Map(o4.id->List(PropositionCondition(EQ(o3.id, "f"), List()))))
//    case x @ _ => //println("TestModel got: " + x)
//  }
//}
