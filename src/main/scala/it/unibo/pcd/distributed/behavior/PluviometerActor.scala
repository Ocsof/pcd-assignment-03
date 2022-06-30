package it.unibo.pcd.distributed.behavior

import akka.actor.typed.scaladsl.*
import akka.actor.typed.scaladsl.adapter.*
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import it.unibo.pcd.distributed.model.{Zone, Point2D}

sealed trait PluviometerCommand
case class pippo() extends Message with PluviometerCommand


object PluviometerActor {
  def apply(position: Zone): Behavior[Unit] =
    Behaviors.setup[Unit] { ctx =>
      ctx.log.info(position.toString)
      Behaviors.same
    }
}
