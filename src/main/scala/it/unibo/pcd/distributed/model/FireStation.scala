package it.unibo.pcd.distributed.model

import it.unibo.pcd.distributed.model.FireStationState.FireStationState

object FireStationState extends Enumeration:
   type FireStationState = Value
   val Free, Busy, Warned = Value

case class FireStation(zoneId: Int, 
                       position: Point2D,
                       state: FireStationState):

   def state_(newState: FireStationState): FireStation =
      FireStation(zoneId, position, newState)
