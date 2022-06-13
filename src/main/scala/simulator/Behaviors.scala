package simulator

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import simulator.model.Body

import scala.collection.mutable


object Actors:

  enum Message:
    case StartSimulation  //quando starta all'inizio l'applicativo
    case PlaySimulation //quando premo play
    case PauseSimulation
    case ComputeVelocityRequest(bodies: mutable.Seq[Body], replyTo: ActorRef[Message])
    case ComputePositionRequest(boundary: Boundary, replyTo: ActorRef[Message])
    case ComputeVelocityResponse(body: Body)
    case ComputePositionResponse(body: Body)
    case StartGUI
    case UpdateGUI(bodies: mutable.Seq[Body], virtualTime: Double, iteration: Int, bounds: Boundary)
  export Message.*

  enum MessageBody:
    case pippo
  export MessageBody.*


  enum MessageView:
    case caio
  export MessageView.*

  object SimulatorActor:
    def apply(simulation: Simulation, withGui: Boolean): Behavior[Message] =
    Behaviors.setup(new SimulatorActor(_, simulation, withGui))

  class SimulatorActor(context: ActorContext[Message], val simulation: Simulation, val withGui: Boolean)
    extends AbstractBehavior[Actors.Message](context):
    import Actors.*
    var responseCounter = 0
    var vt = 0.0
    var actorsList: mutable.Seq[ActorRef[Message]] = mutable.Seq.empty
    var view: Option[ActorRef[Message]] = None
    override def onMessage(msg: Message): Behavior[Message] = msg match {
      case StartSimulation =>
        //se opzione gui abilitata:
        if withGui then {view = Some(context.spawn(ViewActor(), "view-actor")); view.get ! StartGUI}
        for (n <- 0 until simulation.numBodies)
          val newActor = context.spawn(BodyActor(simulation.bodies(n)), "actor-number-"+ n.toString)
          actorsList = actorsList :+ newActor
          newActor ! ComputeVelocityRequest(simulation.bodies, context.self)
        Behaviors.same
      case StopSimulation => ???
      case ComputeVelocityResponse(result) =>
        simulation.bodies(result.id).vel = result.vel
        responseCounter = responseCounter + 1
        if (responseCounter == simulation.bodies.size)
          responseCounter = 0
          actorsList.foreach(bodyActor => bodyActor ! ComputePositionRequest(simulation.boundary, context.self))
        Behaviors.same
      case ComputePositionResponse(result) =>
        simulation.bodies(result.id).pos = result.pos
        responseCounter = responseCounter + 1
        if(responseCounter == simulation.bodies.size)
          responseCounter = 0
          vt = vt + 0.001 /* virtual time step */
          view.get ! UpdateGUI(simulation.bodies, vt, simulation.iteration, simulation.boundary)
          if (simulation.iteration != 0)
            simulation.iteration = simulation.iteration - 1
            actorsList.foreach(y => y ! ComputeVelocityRequest(simulation.bodies, context.self))
            Behaviors.same
          else Behaviors.stopped
        Behaviors.same
    }

  object ViewActor:
    def apply(): Behavior[Message] =
      Behaviors.setup(new ViewActor(_))

  class ViewActor(context: ActorContext[Message]) extends AbstractBehavior[Message](context):
    override def onMessage(msg: Message): Behavior[Message] = msg match {
      case StartGUI => ???
      case UpdateGUI(bodies, vt, iteration, bounds) => ???
      case _ => Behaviors.same //todo: che fare se ti arriva un'altro messaggio?
    }


  object BodyActor:
    def apply(body: Body): Behavior[Message] = Behaviors.setup(new BodyActor(_, body: Body))

  class BodyActor(context: ActorContext[Message], val body: Body) extends AbstractBehavior[Message](context):
    override def onMessage(msg: Message): Behavior[Message] = msg match {
      case ComputeVelocityRequest(bodies, ref) =>
        //computeBodyVelocity  //todo: implementare il task nel model
        ref ! ComputeVelocityResponse(body)
        Behaviors.same
      case ComputePositionRequest(boundary, ref) =>
        //computeBodyPosition //todo: implementare il task nel model
        ref ! ComputePositionResponse(body)
        Behaviors.same
      case _ => Behaviors.same //todo: che fare se ti arriva un'altro messaggio?
    }


