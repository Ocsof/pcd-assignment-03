package it.unibo.pcd.distributed.view

import akka.actor.typed.ActorRef
import akka.actor.typed.receptionist.Receptionist
import it.unibo.pcd.distributed.behavior.{FreeZone, ViewActorCommand}
import it.unibo.pcd.distributed.model.{FireStation, Pluviometer, Zone}


trait View:
  def width: Int
  def height: Int
  def updatePluviometer(pluviometer: Pluviometer): Unit
  def updateFireStation(fireStation: FireStation): Unit
  def freeZonePressed(zoneId: Int): Unit
  def freeFireStationOfZone(zoneId: Int): Unit


object View:
  def apply(width: Int, height: Int, zones: List[Zone], viewActor: ActorRef[ViewActorCommand]): View =
    ViewImpl(width, height, zones, viewActor)

  /**
   * Implementation of View trait
   */
  private class ViewImpl(override val width: Int,
                         override val height: Int,
                         val zones: List[Zone], //todo: dubbio??? perchè qui non ci va l'override??
                         val viewActor: ActorRef[ViewActorCommand]) extends View:
    val frame: SwingControlPanel = SwingControlPanel(this, zones)

    override def updatePluviometer(pluviometer: Pluviometer): Unit =
      frame.updatePluviometer(pluviometer)

    override def updateFireStation(fireStation: FireStation): Unit =
      frame.updateFireStation(fireStation)

    override def freeZonePressed(zoneId: Int): Unit =
      viewActor ! FreeZone(zoneId)

    override def freeFireStationOfZone(zoneId: Int): Unit =
      frame.freeFireStationOfZone(zoneId)
