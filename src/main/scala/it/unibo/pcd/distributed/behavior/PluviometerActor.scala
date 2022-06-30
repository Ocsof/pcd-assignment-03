package it.unibo.pcd.distributed.behavior

import akka.actor.typed.scaladsl.*
import akka.actor.typed.scaladsl.adapter.*
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import it.unibo.pcd.distributed.model.{Zone, Point2D}
import akka.actor.typed.receptionist.*
import scala.util.Random

sealed trait PluviometerCommand
case class Update() extends Message with PluviometerCommand


object PluviometerActor {
  def sensorRead: Double = Random.between(0.0, 10.0)

  def apply(position: Point2D,
            zoneId: String): Behavior[SensorCommand | Receptionist.Listing] =
    Behaviors.setup[SensorCommand | Receptionist.Listing](ctx => {
      ctx.system.receptionist ! Receptionist.Subscribe(ServiceKey[FireStationCommand]("fireStation" + zoneId), ctx.self)
      Behaviors.withTimers(timers => {
        Behaviors.receiveMessage(msg => {
          msg match
            case Update() =>
              println("Update sensor")
              sensorRead match
                // se update > 7 --> errore
                case _ > 7 => ctx.system.receptionist ! Receptionist.Find(ServiceKey[FireStationCommand]("fireStation" + zoneId), ctx.self)
            // recepionist allarme a chi gestisce il messaggio allarme
            // se update < 7 --> tutto regolare
            case Receptionist.Listing(stations) =>
              Behaviors.same

            case _ =>
              println("Tutto regolare Update")
              timers.startSingleTimer(Update(), 5000.millis)
              Behaviors.same
            // caso di errore futuro da gestire...
            case FoundFireStationsToSendAlarmTo =>
            case other => Behaviors.stopped
        })
      })

    })

}

