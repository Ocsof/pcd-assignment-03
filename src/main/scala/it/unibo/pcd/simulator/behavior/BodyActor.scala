package it.unibo.pcd.simulator.behavior

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import scala.collection.mutable

import it.unibo.pcd.simulator.behavior.BodyActor.BodyActorCommand.{ComputePositionRequest, ComputeVelocityRequest}
import it.unibo.pcd.simulator.model.model.{Body, Boundary}
import it.unibo.pcd.simulator.behavior.RootActor.RootActorCommand
import it.unibo.pcd.simulator.behavior.RootActor.RootActorCommand.*

  object BodyActor:
    enum BodyActorCommand:
      case ComputeVelocityRequest(bodies: mutable.Seq[Body], replyTo: ActorRef[RootActorCommand])
      case ComputePositionRequest(boundary: Boundary, replyTo: ActorRef[RootActorCommand])
    export BodyActorCommand.*
    def apply(body: Body): Behavior[BodyActorCommand] = Behaviors.setup(new BodyActor(_, body))

  class BodyActor(context: ActorContext[BodyActor.BodyActorCommand], val body: Body)
    extends AbstractBehavior[BodyActor.BodyActorCommand](context):
    override def onMessage(msg: BodyActor.BodyActorCommand): Behavior[BodyActor.BodyActorCommand] = msg match {
      case ComputeVelocityRequest(bodies, ref) =>
        body.computeBodyVelocity(bodies)
        ref ! ComputeVelocityResponse(body)
        Behaviors.same
      case ComputePositionRequest(boundary, ref) =>
        body.computeBodyPosition(boundary)
        ref ! ComputePositionResponse(body)
        Behaviors.same
      case _ => Behaviors.same
    }
