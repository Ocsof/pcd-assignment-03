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
import akka.actor.typed.scaladsl.LoggerOps

import scala.collection.immutable.Set

sealed trait PluviometerCommand
case class Update() extends Message with PluviometerCommand
private case class FireStationList(listing: Set[ActorRef[FireStationCommand]]) extends PluviometerCommand

val pluviometerService = ServiceKey[PluviometerCommand]("pluviometerService")

object PluviometerActor {
  def sensorRead: Double = Random.between(0.0, 10.0)

  def apply(pluviometer: Pluviometer, zoneFireStations: Set[ActorRef[FireStationCommand]]): Behavior[PluviometerCommand | Receptionist.Listing] =
    Behaviors.setup[PluviometerCommand | Receptionist.Listing](ctx => {
      //todo registrarsi al receptionist
      Behaviors.withTimers(timers => {
        Behaviors.receiveMessage ( msg => {
          ctx.log.info2("{}: received message {}", ctx.self.path.name, msg)
          msg match
            case Update() =>
              if sensorRead > 7 then // se update > 7 --> allarme
                ctx.log.info("{}: alarm detected", ctx.self.path.name)
                if zoneFireStations.nonEmpty then
                  zoneFireStations.foreach(_ ! Alarm())
              timers.startSingleTimer(Update(), 5000.millis)
              Behaviors.same
            // receptionist allarme a chi gestisce il messaggio allarme
            // se update < 7 --> tutto regolare
            case FireStationList(listing) =>
              ctx.log.info2("{}: new set of notifiable actors of length {}", ctx.self.path.name, listing.size)
              PluviometerActor(pluviometer, listing)

            case _ =>
              ctx.log.info("Error")
              Behaviors.stopped
        })
      })

    })

}

