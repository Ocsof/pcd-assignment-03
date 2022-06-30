package it.unibo.pcd.distributed.behavior

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.pcd.distributed.model.{FireStation, Point2D, ZoneState}
import it.unibo.pcd.distributed.model.ZoneState.ZoneState

object FireStationActor {

  trait FireStationCommand
  case class Alarm() extends Message with FireStationCommand
  case class Free() extends Message with FireStationCommand
  //todo: implementare il tipo di messaggio "Richiesta di stato zona": StateRequestMessage
  case class RequestState(replyTo: ActorRef[StateRequestMessage]) extends Message with FireStationCommand

  val fireStationService: ServiceKey[FireStationCommand] = ServiceKey[FireStationCommand]("fireStationService")

  def apply(firestation: FireStation): Behavior[FireStationCommand | Receptionist.Listing] =
    Behaviors.setup[FireStationCommand | Receptionist.Listing](ctx => {
        ctx.system.receptionist ! Receptionist.Register(fireStationService, ctx.self)
        Behaviors.receiveMessage(msg => {
          msg match
            case Alarm() =>
              println("Alarm received in zone "+ firestation.zoneId)
              FireStationActor(FireStation(firestation, ZoneState.Alarm))

            case Free() =>
              println("Free zone "+ firestation.zoneId)
              FireStationActor(FireStation(firestation, ZoneState.Free))

            case RequestState(replyTo) =>
              ctx.log.debug("RequestState")
              replyTo ! ResponseState(firestation)
              Behaviors.same
            case _ =>
              println("Error")
              Behaviors.stopped
        })
    })
}
