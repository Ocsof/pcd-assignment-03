
package it.unibo.pcd.distributed.behavior

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors

object ViewGuardian {
  def apply(width: Int, height: Int): Behavior[Nothing] =
    Behaviors.setup[Nothing] { ctx =>
      val viewActor = ctx.spawn(ViewActor(width, height), name = "View")
      Behaviors.receiveMessage[Nothing] {
        case _ =>
          ctx.log.info("Error")
          Behaviors.stopped
      }
      //todo come gestire sta cosa che deve tornare un behavior -> il suo compito è solamente
    }.narrow
}
