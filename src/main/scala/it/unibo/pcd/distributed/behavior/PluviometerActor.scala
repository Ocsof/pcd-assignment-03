package it.unibo.pcd.distributed.behavior

import akka.actor.typed.scaladsl.*
import akka.actor.typed.scaladsl.adapter.*
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import it.unibo.pcd.distributed.model.{Pluviometer, Point2D, Zone}
import akka.actor.typed.receptionist.*
import it.unibo.pcd.distributed.behavior.fireStationService
import it.unibo.pcd.distributed.behavior.FireStationCommand
import concurrent.duration.DurationInt
import scala.util.Random

sealed trait PluviometerCommand
case class Update() extends Message with PluviometerCommand
private case class ListingResponse(listing: Receptionist.Listing) extends PluviometerCommand

val pluviometerService = ServiceKey[PluviometerCommand]("pluviometerService")

object PluviometerActor {
  def sensorRead: Double = Random.between(0.0, 10.0)

  def apply(pluviometer: Pluviometer): Behavior[PluviometerCommand | Receptionist.Listing] =
    Behaviors.setup[PluviometerCommand | Receptionist.Listing](ctx => {
      //todo registrarsi al receptionist
      ctx.system.receptionist ! Receptionist.Register(pluviometerService, ctx.self)
      val fireStationZoneService = fireStationService(pluviometer.zoneId)
      Behaviors.withTimers(timers => {
        Behaviors.receiveMessage[PluviometerCommand | Receptionist.Listing](msg => {
          msg match
            case Update() =>
              ctx.log.info("Update sensor" + pluviometer.zoneId)
              if sensorRead > 7 then // se update > 7 --> allarme
                ctx.system.receptionist ! Receptionist.Find(fireStationZoneService, ctx.self)
                timers.startSingleTimer(Update(), 5000.millis)
              Behaviors.same
            // receptionist allarme a chi gestisce il messaggio allarme
            // se update < 7 --> tutto regolare
            case fireStationZoneService.Listing(fireStations) =>
              ctx.log.info("Alarm sent to fire station")
              fireStations.last ! Alarm()
              Behaviors.same

            case _ =>
              ctx.log.error("Error")
              Behaviors.stopped
        })
      })

    })

}

