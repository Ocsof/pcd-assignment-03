package it.unibo.pcd.distributed.behavior

import akka.actor.typed.scaladsl.*
import akka.actor.typed.scaladsl.adapter.*
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import it.unibo.pcd.distributed.model.{Pluviometer, Point2D, Zone}
import akka.actor.typed.receptionist.*
import it.unibo.pcd.distributed.behavior.FireStationActor.{FireStationCommand, fireStationService}
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
      Behaviors.withTimers(timers => {
        Behaviors.receiveMessage[PluviometerCommand | Receptionist.Listing](msg => {
          msg match
            case Update() =>
              println("Update sensor")
              if sensorRead > 7 then // se update > 7 --> allarme
                ctx.system.receptionist ! Receptionist.Find(ServiceKey[FireStationCommand]("fireStation" + pluviometer.zoneId), ctx.self)
                timers.startSingleTimer(ctx.self ! Update(), 5000.millis)
              Behaviors.same
            // receptionist allarme a chi gestisce il messaggio allarme
            // se update < 7 --> tutto regolare
            case fireStationService.Listing(fireStations) =>
              Behaviors.same

            case _ =>
              println("Error")
              Behaviors.stopped
        })
      })

    })

}

