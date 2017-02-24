package sp.festo

import akka.actor._


object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)

  cluster.registerOnMemberUp {
    // Add root actors used in node here
    println("festo node has joined the cluster")
    system.actorOf(OPC.props, "OPC")
    system.actorOf(OPMakerFesto.props, "opMakerFesto")
    system.actorOf(ProductAggregator.props, "ProductAggregator")
    system.actorOf(ResourceAggregator.props, "ResourceAggregator")
  }

  cluster.registerOnMemberRemoved{
    println("festo node has been removed from the cluster")
  }


  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  system.terminate()

}
