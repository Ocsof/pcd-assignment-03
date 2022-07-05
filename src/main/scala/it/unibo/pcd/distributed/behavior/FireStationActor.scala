package it.unibo.pcd.distributed.behavior

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.pcd.distributed.model.{FireStation, Point2D, ZoneState}
import it.unibo.pcd.distributed.model.ZoneState.*
import akka.actor.typed.scaladsl.LoggerOps


trait FireStationCommand
case class AlarmFireStation() extends Message with FireStationCommand
case class FreeZone() extends Message with FireStationCommand
case class FirestationViewList(listing: Set[ActorRef[ViewActorCommand]]) extends Message with FireStationCommand

def fireStationService(zoneId: Int): ServiceKey[FireStationCommand] = ServiceKey("fireStationService" + zoneId)

object FireStationActor:
  def apply(firestation: FireStation): Behavior[FireStationCommand | Receptionist.Listing] =
    Behaviors.setup[FireStationCommand | Receptionist.Listing](ctx => {
        ctx.system.receptionist ! Receptionist.Register(fireStationService(firestation.zoneId), ctx.self)

        Behaviors.receiveMessage(msg => {
          ctx.log.info2("{}: received message {}", ctx.self.path.name, msg)
          msg match
            case AlarmFireStation() =>
              FireStationActor(FireStation(firestation, ZoneState.Alarm))

            case FreeZone() =>
              FireStationActor(FireStation(firestation, ZoneState.Free))

            case FirestationViewList(listing) => //invio alla view che si è registrata il mio stato
              ctx.log.info2("{}: new set of Views of length {}", ctx.self.path.name, listing.size)
              listing.foreach(_ ! FirestationState(firestation))
              Behaviors.same

            case _ =>
              ctx.log.info("Error")
              Behaviors.stopped
        })
    })

