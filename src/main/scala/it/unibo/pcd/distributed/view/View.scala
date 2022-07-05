package it.unibo.pcd.distributed.view

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist
import it.unibo.pcd.distributed.behavior.ViewActorCommand
import it.unibo.pcd.distributed.model.{FireStation, Pluviometer, Zone}


trait View:
  def width: Int
  def height: Int
  def updatePluviometer(pluviometer: Pluviometer): Unit
  def updateFireStation(fireStation: FireStation): Unit
  def manageZonePressed(): Unit
  def fixZonePressed(): Unit

object View:
  def apply(width: Int, height: Int, zones: List[Zone], viewActor: ActorRef[ViewActorCommand]): View =
    ViewImpl(width, height, zones, viewActor)

  /**
   * Implementation of View trait
   */
  private class ViewImpl(override val width: Int,
                         override val height: Int,
                         override val zones: List[Zone],
                         val viewActor: ActorRef[ViewActorCommand]) extends View:
    val frame: SwingControlPanel = SwingControlPanel(this, zones)

    override def updatePluviometer(pluviometer: Pluviometer): Unit =
      frame.updatePluviometer(pluviometer)

    override def updateFireStation(fireStation: FireStation): Unit =
      frame.updateFireStation(fireStation)

    override def fixZonePressed(): Unit =
      viewActor ! FixZone

    override def manageZonePressed(): Unit =
      viewActor ! ManageZone