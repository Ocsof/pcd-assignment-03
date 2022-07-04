package it.unibo.pcd.distributed.behavior

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.pcd.distributed.model.{FireStation, Point2D, ZoneState}
import it.unibo.pcd.distributed.model.ZoneState.*
import it.unibo.pcd.distributed.behavior.StateResponse
import akka.actor.typed.scaladsl.LoggerOps


trait FireStationCommand
case class Alarm() extends Message with FireStationCommand
case class Free() extends Message with FireStationCommand
//todo: implementare il tipo di messaggio "Richiesta di stato zona": StateRequestMessage
case class StateRequest(replyTo: ActorRef[StateResponse[FireStation]]) extends Message with FireStationCommand

def fireStationService(zoneId: Int): ServiceKey[FireStationCommand] = ServiceKey("fireStationService" + zoneId)

object FireStationActor:
  def apply(firestation: FireStation): Behavior[FireStationCommand | Receptionist.Listing] =
    Behaviors.setup[FireStationCommand | Receptionist.Listing](ctx => {
        ctx.system.receptionist ! Receptionist.Register(fireStationService(firestation.zoneId), ctx.self)
        Behaviors.receiveMessage(msg => {
          ctx.log.info2("{}: received message {}", ctx.self.path.name, msg)
          msg match
            case Alarm() =>
              FireStationActor(FireStation(firestation, ZoneState.Alarm))

            case Free() =>
              FireStationActor(FireStation(firestation, ZoneState.Free))

            case StateRequest(replyTo) =>
              ctx.log.info("RequestState")
              replyTo ! StateResponse(firestation)
              Behaviors.same
            case _ =>
              ctx.log.info("Error")
              Behaviors.stopped
        })
    })

