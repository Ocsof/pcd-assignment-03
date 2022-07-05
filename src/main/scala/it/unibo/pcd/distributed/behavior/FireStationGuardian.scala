package it.unibo.pcd.distributed.behavior

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.pcd.distributed.model.FireStation

object FireStationGuardian {
  def apply(fireStation: FireStation): Behavior[Nothing] =
    Behaviors.setup[Receptionist.Listing] { ctx =>
      val fireStationActor = ctx.spawn(FireStationActor(fireStation), "FireStation" + fireStation.zoneId)
      ctx.system.receptionist ! Receptionist.Subscribe(viewService, ctx.self)

      Behaviors.receiveMessagePartial[Receptionist.Listing] {
        case viewService.Listing(listings) => {
          ctx.log.info("{}: received new View Actor from receptionist", ctx.self.path.name)
          fireStationActor ! FirestationViewList(listings)
          Behaviors.same
        }
        case _ =>
          ctx.log.info("Error")
          Behaviors.stopped
      }
    }.narrow
}
