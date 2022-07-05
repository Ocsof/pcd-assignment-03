package it.unibo.pcd.distributed.behavior

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.pcd.distributed.model.{FireStation, Pluviometer}


trait ViewActorCommand
case class PluviometerState(pluviometer: Pluviometer) extends Message with ViewActorCommand
case class FirestationState(fireStation: FireStation) extends Message with ViewActorCommand
case class AlarmTheView(pluviometer: Pluviometer) extends Message with ViewActorCommand

val viewService: ServiceKey[ViewActorCommand] = ServiceKey("viewService")

val view: View = Some(View(cityGrid.width + 600, cityGrid.height + 200, ctx.self))

object ViewActor {

  def apply(width: Int, height: Int,
            pluviometers: Set[Pluviometer] = Set.empty,
            firestations: Map[FireStation, ActorRef[FireStationCommand]] = Map.empty,
            view: Option[View] = None): Behavior[ViewActorCommand] =
    Behaviors.setup[ViewActorCommand](ctx => {
      view = Some(View(width + 600, height + 200, ctx.self))
      ctx.system.receptionist ! Receptionist.Register(viewService, ctx.self)
      Behaviors.receiveMessage(msg => {
        ctx.log.info("{}: received message {}", ctx.self.path.name, msg)
        msg match
          case PluviometerState(pluviometer) =>
            val newPluviometers = pluviometers + pluviometer
            
            ViewActor(width, height, newPluviometers, firestations)
          //aggiungi il pluviometro alla lista e ridisegna la gui
          case FirestationState(fireStation) => //aggiungi la firestation alla lista e ridisegna la gui

          case AlarmTheView(pluviometer: Pluviometer) => ???
          case FreeFirestation(zoneId: Int) => fireStationService(zoneId)
          case FreeNotifiedByOtherGui() =>
          case _ =>
            ctx.log.info("Error")
            Behaviors.stopped
      })
    })

}