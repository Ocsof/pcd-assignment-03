package it.unibo.pcd.distributed.behavior

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.pcd.distributed.model.FireStation

object FireStationGuardian {
  def apply(fireStation: FireStation): Behavior[Nothing] =
    Behaviors.setup[Receptionist.Listing] { ctx =>
      val fireStationActor = ctx.spawn(FireStationActor(fireStation), "FireStation" + fireStation.zoneId)

      Behaviors.receiveMessagePartial[Receptionist.Listing] {
        case _ => 
          ctx.log.info("Error")
          Behaviors.stopped
          
      }
    }.narrow
}
