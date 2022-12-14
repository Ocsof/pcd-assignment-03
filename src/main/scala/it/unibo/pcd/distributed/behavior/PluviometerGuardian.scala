package it.unibo.pcd.distributed.behavior

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.pcd.distributed.model.Pluviometer
import akka.actor.typed.scaladsl.LoggerOps
import concurrent.duration.DurationInt

case class Terminate() extends Message

object PluviometerGuardian {
  def apply(pluviometer: Pluviometer): Behavior[Nothing] =
    Behaviors.setup[Receptionist.Listing | Terminate] { ctx =>
      val pluviometerZoneService = pluviometerService(pluviometer.zoneId)
      val fireStationZoneService = fireStationService(pluviometer.zoneId)
      val pluviometerActor = ctx.spawn(PluviometerActor(pluviometer, Set.empty, Set.empty, Set.empty, 0, List.empty), "Pluviometer"+pluviometer.zoneId)
      pluviometerActor ! Update()
      ctx.system.receptionist ! Receptionist.Subscribe(fireStationZoneService, ctx.self)
      ctx.system.receptionist ! Receptionist.Subscribe(pluviometerZoneService, ctx.self)
      ctx.system.receptionist ! Receptionist.Subscribe(viewService, ctx.self)
      Behaviors.withTimers(timers => {
        if ctx.system.address.port.get == 25555 then
          timers.startSingleTimer(Terminate(), 10000.millis)
        Behaviors.receiveMessagePartial[Receptionist.Listing | Terminate]( msg => {
          ctx.log.info2("{}: received message {}", ctx.self.path.name, msg)
          msg match
          case fireStationZoneService.Listing(listings) =>
            ctx.log.info2("{}: received fire station listing from receptionist for zone {}", ctx.self.path.name, pluviometer.zoneId)
            pluviometerActor ! FireStationList(listings)
            Behaviors.same

          case pluviometerZoneService.Listing(listings) =>
            ctx.log.info2("{}: received pluviometer listing from receptionist for zone {}", ctx.self.path.name, pluviometer.zoneId)
            pluviometerActor ! PluviometerList(listings)
            Behaviors.same

          case viewService.Listing(listings) =>
            ctx.log.info("{}: received view listing from receptionist", ctx.self.path.name)
            pluviometerActor ! ViewList(listings)
            Behaviors.same

          case Terminate() => 
            ctx.log.info("Error")
            ctx.system.terminate()
            Behaviors.stopped
          
        })
      })
    }.narrow
}
