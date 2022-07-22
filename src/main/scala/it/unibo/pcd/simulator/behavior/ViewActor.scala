package it.unibo.pcd.simulator.behavior

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import scala.collection.mutable

import it.unibo.pcd.simulator.behavior.RootActor.RootActorCommand
import it.unibo.pcd.simulator.behavior.ViewActor.ViewActorCommand
import it.unibo.pcd.simulator.behavior.ViewActor.ViewActorCommand.{StartGUI, UpdateGUI}
import it.unibo.pcd.simulator.view.ViewController
import it.unibo.pcd.simulator.model.model.{Body, Boundary}

  object ViewActor:
    enum ViewActorCommand:
      case StartGUI
      case UpdateGUI(bodies: mutable.Seq[Body], virtualTime: Double, iteration: Int, bounds: Boundary)
    export ViewActorCommand.*
    def apply(root: ActorRef[RootActorCommand]): Behavior[ViewActorCommand] =
      Behaviors.setup(new ViewActor(_, root))

  class ViewActor(context: ActorContext[ViewActor.ViewActorCommand], root: ActorRef[RootActorCommand]) extends AbstractBehavior[ViewActorCommand](context):
    var view: ViewController = new ViewController(root)
    override def onMessage(msg: ViewActor.ViewActorCommand): Behavior[ViewActor.ViewActorCommand] = msg match {
      case StartGUI =>
        view.startView()
        Behaviors.same
      case UpdateGUI(bodies, vt, iteration, bounds) =>
        view.display(bodies, vt, iteration, bounds)
        Behaviors.same
      case _ => Behaviors.same
    }
