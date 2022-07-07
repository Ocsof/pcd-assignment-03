
package it.unibo.pcd.distributed.behavior

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.pcd.distributed.model.Zone

object ViewGuardian {
  def apply(width: Int, height: Int, zones: List[Zone]): Behavior[Nothing] =
    Behaviors.setup[Receptionist.Listing] { ctx =>
      val viewActor = ctx.spawnAnonymous(Behaviors.setup(new ViewActor(_, width, height, zones)))
      ctx.system.receptionist ! Receptionist.Subscribe(viewService, ctx.self)
      Behaviors.receiveMessagePartial[Receptionist.Listing]( msg => {
        ctx.log.info("{}: received message {}", ctx.self.path.name, msg)
        msg match
          case viewService.Listing(listings) => {
            ctx.log.info("{}: received new View listing from receptionist {}", ctx.self.path.name)
            viewActor ! OtherViewList(listings)
            Behaviors.same
          }
          case _ =>
            ctx.log.info("Error")
            Behaviors.stopped
      })
      //todo come gestire sta cosa che deve tornare un behavior -> il suo compito è solamente
    }.narrow
}
