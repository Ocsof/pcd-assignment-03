package it.unibo.pcd.distributed.behavior

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import it.unibo.pcd.distributed.model.{FireStation, Pluviometer, Zone, ZoneState}
import it.unibo.pcd.distributed.view.View


trait ViewActorCommand
case class PluviometerState(pluviometer: Pluviometer) extends Message with ViewActorCommand
case class FirestationState(fireStation: FireStation, fireStationActor: ActorRef[FireStationCommand]) extends Message with ViewActorCommand
case class OtherViewList(listings: Set[ActorRef[ViewActorCommand]]) extends Message with ViewActorCommand
case class AlarmTheView(pluviometer: Pluviometer) extends Message with ViewActorCommand
case class FreeZone(zoneId: Int) extends Message with ViewActorCommand
case class WarnsOtherGuiZoneFree(zoneId: Int) extends Message with ViewActorCommand

val viewService: ServiceKey[ViewActorCommand] = ServiceKey("viewService")


class ViewActor(ctx: ActorContext[ViewActorCommand],
                width: Int, height: Int, zones: List[Zone]) extends AbstractBehavior(ctx):

  //todo: Dubbio??? tenerli Var è necessario? secondo me in questo caso si
  var pluviometers: Set[Pluviometer] = Set.empty
  val view: View = View(width, height, zones)
  view.viewActors = Set(ctx.self)
  var firestationActors: Map[Int, ActorRef[FireStationCommand]] = Map.empty

  ctx.system.receptionist ! Receptionist.Register(viewService, ctx.self)

  override def onMessage(msg: ViewActorCommand): Behavior[ViewActorCommand] =
    ctx.log.info("{}: received message {}", ctx.self.path.name, msg)
    msg match
      case PluviometerState(pluviometer) =>
        //aggiungi il pluviometro alla lista e ridisegna la gui
        pluviometers = pluviometers + pluviometer
        view.updatePluviometer(pluviometer)

      case FirestationState(fireStation, fireStationActor) => //aggiungi la firestation alla lista e ridisegna la gui
        firestationActors = firestationActors + (fireStation.zoneId -> fireStationActor)
        view.updateFireStation(fireStation)

      case OtherViewList(othersViewActor) =>
        view.viewActors = othersViewActor + ctx.self

      case AlarmTheView(pluviometer: Pluviometer) =>
        view.setZoneState(pluviometer.zoneId, ZoneState.Busy)

      case FreeZone(zoneId) =>
        val fireStation = firestationActors.keys.find(_ == zoneId)
        if fireStation.isDefined then
          val actorRef = firestationActors(zoneId) //mando all'actorRef la FreeZone
          actorRef ! FreeZoneFirestation()
        view.setZoneState(zoneId, ZoneState.Free)

      this

