package sp.domain.logic

/**
 * Created by kristofer on 30/09/14.
 */
object IDAbleLogic {

  import sp.domain._
  import sp.system.messages._

  def removeID(id: Set[ID], ids: List[IDAble]) = {
    def removeFrom(item: IDAble): Option[UpdateID] = {
      val newItem = item match {
        case x: Operation => removeIDFromOperation(id, x)
        case x: Thing => removeIDFromThing(id, x)
        case x: SPObject => removeIDFromSPObject(id, x)
        case x: SPSpec => removeIDFromSPSpec(id, x)
        case x: SOPSpec => removeIDFromSOPSpec(id, x)
        case _ => item
      }
      if (newItem == item) None
      else {
        Some(UpdateID(item.id, item.version, newItem))
      }
    }

    val t = ids flatMap removeFrom
    println(s"removeID: $t")
    t
  }




  def removeIDFromThing(id: Set[ID], th: Thing): Thing = {
    val newAttr = removeIDFromAttribute(id, th.attributes)
    val newSV = th.stateVariables flatMap(sv => removeIDFromStateVariable(id, sv))
    if (newAttr == th.attributes && newSV == th.stateVariables)
      th
    else
      th.copy(stateVariables = newSV, attributes = newAttr)
  }

  def removeIDFromOperation(id: Set[ID], op: Operation): Operation = {
    val newAttr = removeIDFromAttribute(id, op.attributes)
    val removeIt = removeIDFromCondition(id, (_: Condition))
    val newCond = op.conditions map removeIt
    if (newAttr == op.attributes && newCond == op.conditions)
      op
    else
      op.copy(conditions = newCond, attributes = newAttr)
  }

  def removeIDFromSPObject(id: Set[ID], obj: SPObject): SPObject = {
    val newAttr = removeIDFromAttribute(id, obj.attributes)
    if (newAttr == obj.attributes)
      obj
    else
      obj.copy(attributes = newAttr)
  }
  def removeIDFromSPSpec(id: Set[ID], obj: SPSpec): SPSpec = {
    val newAttr = removeIDFromAttribute(id, obj.attributes)
    if (newAttr == obj.attributes)
      obj
    else
      obj.copy(attributes = newAttr)
  }

  def removeIDFromSOPSpec(id: Set[ID], obj: SOPSpec): SOPSpec = {
    println(s"id: $id,  a sopspec to check: $obj")
    val newAttr = removeIDFromAttribute(id, obj.attributes)
    val newSOP = obj.sop flatMap(sop => removeIDFromSOP(id, sop))
    if (newAttr == obj.attributes && newSOP == obj.sop)
      obj
    else
      obj.copy(sop = newSOP, attributes = newAttr)
  }

  def removeIDFromSOP(id: Set[ID], sop: SOP): Option[SOP] = {
    def filter(xs: Seq[SOP]) = {
      println(s"id: $id, \n filter: $xs")
      val t = xs flatMap(x => removeIDFromSOP(id, x))
      println(s"res: $t")
      if (t == xs) xs else t
    }

    val t = sop match {
      case h: Hierarchy => {
        if (id.contains(h.operation)) None
        else {
          val newChildren = filter(h.children)
          if (newChildren == sop.children) Some(h)
          else Some(h.modify(newChildren))
        }
      }
      case EmptySOP => Some(EmptySOP)
      case _ => {
        val newChildren = filter(sop.children)
        if (newChildren == sop.children)
          Some(sop)
        else if (newChildren.isEmpty)
          None
        else if (newChildren.size == 1)
          Some(newChildren.head)
        else
          Some(sop.modify(newChildren))
      }
    }

    println(s"sop before: $sop")
    println(s"sop after: $t")

    t
  }

  def removeIDFromStateVariable(id: Set[ID], sv: StateVariable): Option[StateVariable] = {
    if (id.contains(sv.id)) None
    else {
      val newAttr = removeIDFromAttribute(id, sv.attributes)
      Some(sv.copy(attributes = newAttr))
    }
  }

  def removeIDFromCondition(id: Set[ID], cond: Condition): Condition = {
    cond match {
      case c @ PropositionCondition(guard, action, attr) => {
        val newGuard = removeIDFromProposition(id, guard)
        val newActions = removeIDFromAction(id, action)
        val newAttr = removeIDFromAttribute(id, attr)
        PropositionCondition(newGuard, newActions, newAttr)
      }
      case _ => cond
    }
  }

  def removeIDFromProposition(id: Set[ID], prop: Proposition): Proposition = {
    def clean(xs: List[Proposition], make: List[Proposition] => Proposition) = xs match {
      case Nil => None
      case _ => Some(make(xs))
    }
    def removeIDFromStateEvaluator(sv: StateEvaluator): Option[StateEvaluator] = {
      sv match {
        case SVIDEval(svid) => if (id.contains(svid)) None else Some(sv)
        case ValueHolder(v) => {
          removeIDFromAttrValue(id, v) map ValueHolder
        }
        case _ => Some(sv)
      }
    }
    def req(prop: Proposition): Option[Proposition] = {
      prop match {
        case AND(xs) => clean(xs flatMap req, AND.apply)
        case OR(xs) => clean(xs flatMap req, OR.apply)
        case NOT(x) => clean(List(x) flatMap req, AND.apply) // AND will never be used
        case EQ(left, right) => {
          for {
            l <- removeIDFromStateEvaluator(left)
            r <- removeIDFromStateEvaluator(right)
          } yield EQ(l, r)
        }
        case NEQ(left, right) => {
          for {
            l <- removeIDFromStateEvaluator(left)
            r <- removeIDFromStateEvaluator(right)
          } yield NEQ(l, r)
        }
        case _ => Some(prop)
      }

    }

    req(prop) match {
      case Some(x) => x
      case None => AlwaysTrue
    }
  }


  def removeIDFromAction(id: Set[ID], actions: List[Action]): List[Action] = {
    actions flatMap {
      case a @ Action(svid, ValueHolder(v)) => {
        val newV = removeIDFromAttrValue(id, v)
        if (id.contains(svid) || newV == None) None
        else Some(Action(svid, ValueHolder(newV.get)))
      }
      case a @ Action(svid, ASSIGN(assignID)) => {
        if (id.contains(svid) || id.contains(assignID)) None
        else Some(a)
      }
      case a @ Action(svid, _) => {
        if (id.contains(svid)) None
        else Some(a)
      }
    }
  }

  def removeIDFromAttribute(id: Set[ID], attr: SPAttributes): SPAttributes = {
    val updated = reqRemoveFromAttr(id, attr.attrs)
    if (updated == attr.attrs) attr else SPAttributes(updated)
  }
  def removeIDFromAttrValue(id: Set[ID], attrVal: SPAttributeValue): Option[SPAttributeValue] = {
    attrVal match {
      case IDPrimitive(x) => if (id.contains(x)) None else Some(attrVal)
      case MapPrimitive(x) => {
        val upd = reqRemoveFromAttr(id, x)
        Some(MapPrimitive(upd))
      }
      case ListPrimitive(xs) => {
        val upd  = xs flatMap ((v => removeIDFromAttrValue(id, v)))
        Some(ListPrimitive(upd))
      }
      case OptionAsPrimitive(x) => x flatMap (v => removeIDFromAttrValue(id, v))
      case _ => Some(attrVal)
    }
  }
  private def reqRemoveFromAttr(id: Set[ID], keyVal: Map[String, SPAttributeValue]): Map[String, SPAttributeValue] = {
    val markID = keyVal map { case (key, attr) =>
      val newAttr = removeIDFromAttrValue(id, attr)
      newAttr match {
        case Some(x) => key -> x
        case None => "remove!!!!" -> StringPrimitive("")
      }
    }
    markID - "remove!!!!"
  }

}
