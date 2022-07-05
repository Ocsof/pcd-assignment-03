package it.unibo.pcd.distributed.behavior

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import it.unibo.pcd.distributed.model.{FireStation, Pluviometer, Zone}
import it.unibo.pcd.distributed.view.View


trait ViewActorCommand
case class PluviometerState(pluviometer: Pluviometer) extends Message with ViewActorCommand
case class FirestationState(fireStation: FireStation, fireStationActor: ActorRef[FireStationCommand]) extends Message with ViewActorCommand
case class AlarmTheView(pluviometer: Pluviometer) extends Message with ViewActorCommand

val viewService: ServiceKey[ViewActorCommand] = ServiceKey("viewService")


class ViewActor(ctx: ActorContext[ViewActorCommand],
                width: Int, height: Int, zones: List[Zone]) extends AbstractBehavior(ctx):

  val view: View = View(width, height, zones, ctx.self)
  val pluviometers: Set[Pluviometer] = Set.empty
  val firestations: Map[FireStation, ActorRef[FireStationCommand]] = Map.empty

  ctx.system.receptionist ! Receptionist.Register(viewService, ctx.self)

  override def onMessage(msg: ViewActorCommand): Behavior[ViewActorCommand] =
    ctx.log.info("{}: received message {}", ctx.self.path.name, msg)
    msg match
      case PluviometerState(pluviometer) =>
        //aggiungi il pluviometro alla lista e ridisegna la gui
        val newPluviometers = pluviometers + pluviometer
        view.updatePluviometer(pluviometer)

      case FirestationState(fireStation, fireStationActor) => //aggiungi la firestation alla lista e ridisegna la gui
        val newFirestations = firestations + (fireStation -> fireStationActor)
        view.updateFireStation(fireStation)

      case AlarmTheView(pluviometer: Pluviometer) => ???
          /*
          case FreeFirestation(zoneId: Int) => fireStationService(zoneId)
          case FreeNotifiedByOtherGui() =>
          case _ =>
            ctx.log.info("Error")
            Behaviors.stopped
          */
      this

