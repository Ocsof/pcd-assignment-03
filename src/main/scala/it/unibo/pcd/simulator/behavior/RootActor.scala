package it.unibo.pcd.simulator.behavior

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import scala.collection.mutable

import it.unibo.pcd.simulator.model.model.{Body, Boundary, Simulation}
import it.unibo.pcd.simulator.behavior.BodyActor.BodyActorCommand
import it.unibo.pcd.simulator.behavior.BodyActor.BodyActorCommand.*
import it.unibo.pcd.simulator.behavior.RootActor.RootActorCommand.{StartSimulation, StopSimulation, ComputePositionResponse, ComputeVelocityResponse}
import it.unibo.pcd.simulator.behavior.ViewActor.ViewActorCommand
import it.unibo.pcd.simulator.behavior.ViewActor.ViewActorCommand.*


  object RootActor:
    enum RootActorCommand:
      case StartSimulation   //quando starta all'inizio l'applicativo
      case StopSimulation
      case ComputeVelocityResponse(body: Body)
      case ComputePositionResponse(body: Body)
    export RootActorCommand.*
    def apply(simulation: Simulation, withGui: Boolean): Behavior[RootActorCommand] =
    Behaviors.setup(new RootActor(_, simulation, withGui))

  class RootActor(context: ActorContext[RootActor.RootActorCommand], val simulation: Simulation, val withGui: Boolean)
    extends AbstractBehavior[RootActor.RootActorCommand](context):
    var responseCounter = 0
    var vt = 0.0
    var bodyActors: mutable.Seq[ActorRef[BodyActorCommand]] = mutable.Seq.empty
    var view: Option[ActorRef[ViewActorCommand]] = None
    override def onMessage(msg: RootActor.RootActorCommand): Behavior[RootActor.RootActorCommand] = msg match {
      case StartSimulation =>
        //se opzione gui abilitata: ViewActor(context.self)
        if withGui then {view = Some(context.spawn(ViewActor(context.self), "view-actor")); view.get ! StartGUI}
        for (n <- 0 until simulation.numBodies)
          val bodyActor = context.spawn(BodyActor(simulation.bodies(n)), s"body-actor-$n")
          bodyActors = bodyActors :+ bodyActor
          bodyActor ! ComputeVelocityRequest(simulation.bodies, context.self)
        Behaviors.same
      case StopSimulation =>
        context.log.debug("Received StopSimulation")
        bodyActors.foreach(bodyActor => bodyActor ! StopBodyActor)
        Behaviors.same  //Behaviors.stopped
      case ComputeVelocityResponse(result) =>
        simulation.bodies(result.id).vel = result.vel
        responseCounter = responseCounter + 1
        if (responseCounter == simulation.bodies.size)
          responseCounter = 0
          bodyActors.foreach(bodyActor => bodyActor ! ComputePositionRequest(simulation.boundary, context.self))
        Behaviors.same
      case ComputePositionResponse(result) =>
        simulation.bodies(result.id).pos = result.pos
        responseCounter = responseCounter + 1
        if(responseCounter == simulation.bodies.size)
          responseCounter = 0
          vt = vt + 0.001 /* virtual time step */
          if withGui then view.get ! UpdateGUI(simulation.bodies, vt, simulation.iteration, simulation.boundary)
          if (simulation.iteration != 0)
            simulation.iteration = simulation.iteration - 1
            bodyActors.foreach(y => y ! ComputeVelocityRequest(simulation.bodies, context.self))
            Behaviors.same
          else Behaviors.stopped
        Behaviors.same
    }







