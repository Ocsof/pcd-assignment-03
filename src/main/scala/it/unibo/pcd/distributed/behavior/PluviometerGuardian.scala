package it.unibo.pcd.distributed.behavior

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.pcd.distributed.model.Pluviometer
import akka.actor.typed.scaladsl.LoggerOps

object PluviometerGuardian {
  def apply(pluviometer: Pluviometer): Behavior[Nothing] =
    Behaviors.setup[Receptionist.Listing] { ctx =>
      val fireStationZoneService = fireStationService(pluviometer.zoneId)
      val pluviometerActor = ctx.spawn(PluviometerActor(pluviometer, Set.empty, Set.empty), "Pluviometer"+pluviometer.zoneId)
      pluviometerActor ! Update()
      ctx.system.receptionist ! Receptionist.Subscribe(fireStationZoneService, ctx.self)
      ctx.system.receptionist ! Receptionist.Subscribe(viewService, ctx.self)

      Behaviors.receiveMessagePartial[Receptionist.Listing]( msg => {
        ctx.log.info2("{}: received message {}", ctx.self.path.name, msg)
        msg match
        case fireStationZoneService.Listing(listings) => {
          ctx.log.info2("{}: received fire station listing from receptionist for zone {}", ctx.self.path.name, pluviometer.zoneId)
          pluviometerActor ! FireStationList(listings)
          Behaviors.same
        }
        case viewService.Listing(listings) => {
          ctx.log.info2("{}: received fire station listing from receptionist for zone {}", ctx.self.path.name, pluviometer.zoneId)
          pluviometerActor ! PluviometerViewList(listings)
          Behaviors.same
        }
        case _ => {
          ctx.log.info("Error")
          Behaviors.stopped
        }
      })
    }.narrow
}
