package it.unibo.pcd.simulator.behavior

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.mutable
import it.unibo.pcd.simulator.model.model.{Body, Boundary, Simulation}
import it.unibo.pcd.simulator.behavior.BodyActor.BodyActorCommand
import it.unibo.pcd.simulator.behavior.BodyActor.BodyActorCommand.*
import it.unibo.pcd.simulator.behavior.RootActor.RootActorCommand.{ComputePositionResponse, ComputeVelocityResponse, PauseSimulation, StartSimulation}
import it.unibo.pcd.simulator.behavior.ViewActor.ViewActorCommand
import it.unibo.pcd.simulator.behavior.ViewActor.ViewActorCommand.*


  object RootActor:
    enum RootActorCommand:
      case StartSimulation   //quando starta all'inizio l'applicativo
      case PauseSimulation
      case ResumeSimulation
      case ComputeVelocityResponse(body: Body)
      case ComputePositionResponse(body: Body)
    export RootActorCommand.*
    
    def apply(simulation: Simulation, withGui: Boolean,
              responseCounter: Int,
              vt: Double,
              bodyActors: mutable.Seq[ActorRef[BodyActorCommand]],
              view: Option[ActorRef[ViewActorCommand]],
              startTime: Long): Behavior[RootActorCommand] =
    Behaviors.setup(new RootActor(_, simulation, withGui, responseCounter, vt, bodyActors, view, startTime))

  class RootActor(context: ActorContext[RootActor.RootActorCommand],
                  val simulation: Simulation, val withGui: Boolean,
                  var responseCounter: Int,
                  var vt: Double,
                  var bodyActors: mutable.Seq[ActorRef[BodyActorCommand]],
                  var view: Option[ActorRef[ViewActorCommand]],
                  val startTime: Long)
    extends AbstractBehavior[RootActor.RootActorCommand](context):

    override def onMessage(msg: RootActor.RootActorCommand): Behavior[RootActor.RootActorCommand] = msg match {
      case StartSimulation =>
        import RootActor.*
        //se opzione gui abilitata: ViewActor(context.self)
        if withGui then {view = Some(context.spawn(ViewActor(context.self), "view-actor")); view.get ! StartGUI}
        for (n <- 0 until simulation.numBodies)
          val bodyActor = context.spawn(BodyActor(simulation.bodies(n)), s"body-actor-$n")
          bodyActors = bodyActors :+ bodyActor
          bodyActor ! ComputeVelocityRequest(simulation.bodies, context.self)
        this

      case PauseSimulation =>
        this.onPause(simulation, withGui, responseCounter, vt, bodyActors, view, startTime)

      case ComputeVelocityResponse(result) =>
        import RootActor.*
        simulation.bodies(result.id).vel = result.vel
        responseCounter = responseCounter + 1
        if (responseCounter == simulation.bodies.size)
          responseCounter = 0
          bodyActors.foreach(bodyActor => bodyActor ! ComputePositionRequest(simulation.boundary, context.self))
        this
      case ComputePositionResponse(result) =>
        import RootActor.*
        simulation.bodies(result.id).pos = result.pos
        responseCounter = responseCounter + 1
        if(responseCounter == simulation.bodies.size)
          responseCounter = 0
          vt = vt + 0.001 /* virtual time step */
          if view.isDefined then view.get ! UpdateGUI(simulation.bodies, vt, simulation.iteration, simulation.boundary)
          if (simulation.iteration != 0)
            simulation.iteration = simulation.iteration - 1
            bodyActors.foreach(y => y ! ComputeVelocityRequest(simulation.bodies, context.self))
            this
          else
            val getTime = System.currentTimeMillis() - startTime
            println(getTime)
            Behaviors.stopped
        else this
      case _ => this
    }

    def onPause(simulation: Simulation, gui: Boolean,
                responseCounter: Int,
                vt: Double,
                bodyActors: mutable.Seq[ActorRef[BodyActorCommand]],
                view: Option[ActorRef[ViewActorCommand]],
                startTime: Long): Behavior[RootActor.RootActorCommand] =
      import RootActor.RootActorCommand
      import RootActor.RootActorCommand.*
      Behaviors.setup( ctx => {
        Behaviors.withStash[RootActorCommand](10000){ stash =>
          Behaviors.receiveMessage{
            case ResumeSimulation =>
              System.out.println("Resume")
              stash.unstashAll(RootActor(simulation, gui, responseCounter, vt, bodyActors, view, startTime))
            case PauseSimulation =>
              System.out.println("Pause")
              Behaviors.same
            case other =>
              stash.stash(other)
              Behaviors.same
          }
        }
      })





