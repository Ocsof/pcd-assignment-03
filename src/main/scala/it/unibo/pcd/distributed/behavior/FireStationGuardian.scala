package it.unibo.pcd.distributed.behavior

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.pcd.distributed.model.FireStation
import concurrent.duration.DurationInt

object FireStationGuardian {
  def apply(fireStation: FireStation): Behavior[Nothing] =
    Behaviors.setup[Receptionist.Listing | Terminate] { ctx =>
      val fireStationActor = ctx.spawn(FireStationActor(fireStation), "FireStation" + fireStation.zoneId)
      ctx.system.receptionist ! Receptionist.Subscribe(viewService, ctx.self)
      Behaviors.withTimers(timers => {
        if ctx.system.address.port.get == 25554 then
          timers.startSingleTimer(Terminate(), 10000.millis)
        Behaviors.receiveMessagePartial[Receptionist.Listing | Terminate] {
          case viewService.Listing(listings) =>
            ctx.log.info("{}: received new View Actor from receptionist", ctx.self.path.name)
            fireStationActor ! FirestationViewList(listings)
            Behaviors.same

          case Terminate() =>
            ctx.log.info("Error")
            ctx.system.terminate()
            Behaviors.stopped

        }
      })
      }.narrow
}
