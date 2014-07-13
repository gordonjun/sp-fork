package sp.system.messages

import akka.actor.Props
import sp.domain._




// Send
case class RegisterRuntimeKind(name: String, props: CreateRuntime => Props, attributes: SPAttributes)
case object GetRuntimeKinds

// receive
case class RuntimeKindInfo(name: String, attributes: SPAttributes)
case class RuntimeKindInfos(runtimes: List[RuntimeKindInfo])

// send
case class CreateRuntime(kind: String, model: String, name: String, settings: Option[SPAttributes])
case object GetRuntimes

// receive
case class RuntimeInfos(runtimes: List[CreateRuntime])


// Messages to talk to various runtimes

trait RuntimeMessage {
  val runtime: String
}

case class SimpleMessage(runtime: String, attributes: SPAttributes) extends RuntimeMessage

