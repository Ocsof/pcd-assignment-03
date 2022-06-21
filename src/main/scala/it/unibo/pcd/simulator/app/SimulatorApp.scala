package it.unibo.pcd.simulator.app

import akka.actor.typed.ActorSystem
import it.unibo.pcd.simulator.behavior.RootActor
import it.unibo.pcd.simulator.behavior.RootActor.RootActorCommand
import it.unibo.pcd.simulator.behavior.RootActor.RootActorCommand.*
import it.unibo.pcd.simulator.model.model.Simulation

  @main
  def main(): Unit = {
    val system: ActorSystem[RootActorCommand] = ActorSystem(RootActor(Simulation(1000, 10000), true), name = "simulation")
    system ! StartSimulation
  }


