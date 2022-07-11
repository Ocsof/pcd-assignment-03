package it.unibo.pcd.distributed.behavior

import akka.actor.typed.scaladsl.*
import akka.actor.typed.scaladsl.adapter.*
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import it.unibo.pcd.distributed.model.{Pluviometer, Point2D, Zone}
import akka.actor.typed.receptionist.*
import it.unibo.pcd.distributed.behavior.fireStationService
import it.unibo.pcd.distributed.behavior.FireStationCommand
import akka.actor.typed.scaladsl.AskPattern.Askable

import concurrent.duration.DurationInt
import scala.util.{Random, Success}
import akka.actor.typed.scaladsl.LoggerOps
import akka.util.Timeout
import it.unibo.pcd.distributed.behavior.PluviometerState.PluviometerState

import scala.collection.immutable.Set

object PluviometerState extends Enumeration:
  type PluviometerState = Value
  val AboveThreshold, BelowThreshold, Unavailable = Value


sealed trait PluviometerCommand
case class Update() extends Message with PluviometerCommand
case class FireStationList(listing: Set[ActorRef[FireStationCommand]]) extends Message with PluviometerCommand
case class PluviometerList(listings: Set[ActorRef[PluviometerCommand]]) extends Message with PluviometerCommand
case class ViewList(listing: Set[ActorRef[ViewActorCommand]]) extends Message with PluviometerCommand
case class IsRainLevelAboveThresholdRequest(replyTo: ActorRef[PluviometerCommand]) extends Message with PluviometerCommand
case class IsRainLevelAboveThresholdResponse(pluviometerState: PluviometerState) extends Message with PluviometerCommand
case class HealPluviometer() extends Message with PluviometerCommand

def pluviometerService(zoneId: Int): ServiceKey[PluviometerCommand] = ServiceKey("pluviometerService" + zoneId)

object PluviometerActor {
  import PluviometerState.*
  //deve essere più piccolo della frequenza degli update
  implicit val timeout: Timeout = 2.seconds

  def sensorRead: Double = Random.between(0.0, 10.0)

  def apply(pluviometer: Pluviometer,
            zonePluviometers: Set[ActorRef[PluviometerCommand]],
            zoneFireStations: Set[ActorRef[FireStationCommand]],
            views: Set[ActorRef[ViewActorCommand]],
            rainLevel: Double,
            pluviometersResponses: List[PluviometerState]): Behavior[PluviometerCommand] =
    Behaviors.setup[PluviometerCommand](ctx => {
      ctx.system.receptionist ! Receptionist.Register(pluviometerService(pluviometer.zoneId), ctx.self)
      Behaviors.withTimers(timers => {
        Behaviors.receiveMessage ( msg => {
          ctx.log.info2("{}: received message {}", ctx.self.path.name, msg)

          msg match
            case Update() =>
              val newRainLevel = sensorRead
              if newRainLevel > pluviometer.threshold then // se update > 7 --> allarme
                zonePluviometers.foreach(pluviometer => ctx.ask(pluviometer, IsRainLevelAboveThresholdRequest.apply){
                  case Success(IsRainLevelAboveThresholdResponse(pluviometerState)) => IsRainLevelAboveThresholdResponse(pluviometerState)
                  case _ => IsRainLevelAboveThresholdResponse(Unavailable)
                })
              timers.startSingleTimer(Update(), 5000.millis)
              PluviometerActor(pluviometer, zonePluviometers, zoneFireStations, views, newRainLevel, pluviometersResponses)

            case IsRainLevelAboveThresholdRequest(otherSensor) =>
              otherSensor ! IsRainLevelAboveThresholdResponse(if rainLevel > pluviometer.threshold then AboveThreshold else BelowThreshold)
              Behaviors.same

            case IsRainLevelAboveThresholdResponse(response) =>
              val newResponses = response :: pluviometersResponses
              if newResponses.size == zonePluviometers.size then
                val percentageAboveThreshold: Double = newResponses.count(_ == AboveThreshold).toDouble / (newResponses.size - newResponses.count(_ == Unavailable)).toDouble
                //println("Zone " + zone + " Ho tutte le risposte: " + sensorResponse + " QUORUM: " + quorum)
                println("[" + pluviometer.zoneId + "] AboveTreshold: " + percentageAboveThreshold + "%")
                if percentageAboveThreshold > 0.5 then
                  ctx.log.info("{}: alarm detected", ctx.self.path.name)
                  zoneFireStations.foreach(_ ! AlarmFireStation())
                  views.foreach(_ ! AlarmTheView(pluviometer))
                PluviometerActor(pluviometer, zonePluviometers, zoneFireStations, views, rainLevel, List.empty)
              else
                PluviometerActor(pluviometer, zonePluviometers, zoneFireStations, views, rainLevel, newResponses)

            // receptionist allarme a chi gestisce il messaggio allarme
            // se update < 7 --> tutto regolare
            case FireStationList(listing) =>
              ctx.log.info2("{}: new set of Firestation of length {}", ctx.self.path.name, listing.size)
              PluviometerActor(pluviometer, zonePluviometers, listing, views, rainLevel, pluviometersResponses)

            case PluviometerList(listing) =>
              ctx.log.info2("{}: new set of Pluviometers of length {}", ctx.self.path.name, listing.size)
              PluviometerActor(pluviometer, listing, zoneFireStations, views, rainLevel, pluviometersResponses)

            case ViewList(listing) => //invio alla view che si è registrata il mio stato
              ctx.log.info2("{}: new set of Views of length {}", ctx.self.path.name, listing.size)
              listing.foreach(_ ! PluviometerModel(pluviometer))
              PluviometerActor(pluviometer, zonePluviometers, zoneFireStations, listing, rainLevel, pluviometersResponses)


            case _ =>
              ctx.log.info("Error")
              Behaviors.stopped
        })
      })

    })
}


