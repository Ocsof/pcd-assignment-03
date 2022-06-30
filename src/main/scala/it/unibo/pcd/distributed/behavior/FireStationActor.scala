package it.unibo.pcd.distributed.behavior

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.pcd.distributed.model.Point2D

object FireStationActor {
  enum State:
    case Free
    case Alarm

  trait FireStationCommand
  case class Alarm(zoneId: Int) extends Message with FireStationCommand
  case class Free(zoneId: Int) extends Message with FireStationCommand
  case class RequestState() extends Message with FireStationCommand

  def apply(position: Point2D,
            zoneId: String,
            state: State): Behavior[FireStationCommand | Receptionist.Listing] =
    Behaviors.setup[FireStationCommand | Receptionist.Listing](ctx => {
        Behaviors.receiveMessage(msg => {
          msg match
            case Alarm() =>
              println("Alarm received in zone "+ zoneId)
              FireStationActor(position, zoneId, State.Alarm)

            case Free() =>
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
}
