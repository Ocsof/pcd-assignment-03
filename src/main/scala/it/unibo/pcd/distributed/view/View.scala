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
  def viewActors: Set[ActorRef[ViewActorCommand]]
  def viewActors_=(viewActors: Set[ActorRef[ViewActorCommand]]): Unit
  def freeZonePressed(zoneId: Int): Unit
  def freeFireStationOfZone(zoneId: Int): Unit


object View:
  def apply(width: Int, height: Int, zones: List[Zone]): View =
    ViewImpl(width, height, zones)

  /**
   * Implementation of View trait
   */
  private class ViewImpl(override val width: Int,
                         override val height: Int,
                         val zones: List[Zone]) extends View:
    val frame: SwingControlPanel = SwingControlPanel(this, zones)
    var _viewActors: Set[ActorRef[ViewActorCommand]] = Set.empty

    override def updatePluviometer(pluviometer: Pluviometer): Unit =
      frame.updatePluviometer(pluviometer)

    override def updateFireStation(fireStation: FireStation): Unit =
      frame.updateFireStation(fireStation)

    override def freeZonePressed(zoneId: Int): Unit =
      this._viewActors.foreach(viewActor => {
        viewActor ! FreeZone(zoneId)
      })

    override def viewActors: Set[ActorRef[ViewActorCommand]] = this._viewActors

    override def viewActors_=(viewActors: Set[ActorRef[ViewActorCommand]]): Unit =
      this._viewActors = viewActors

    override def freeFireStationOfZone(zoneId: Int): Unit =
      frame.freeFireStationOfZone(zoneId)

