package it.unibo.pcd.distributed.model

import it.unibo.pcd.distributed.model.ZoneState.ZoneState

/*
object FireStationState extends Enumeration:
   type FireStationState = Value
   val Free, Busy, Warned = Value
*/

object ZoneState extends Enumeration:
   type ZoneState = Value
   val Free, Busy = Value

case class FireStation(position: Point2D,
                          zoneId: Int,
                          state: ZoneState)
object FireStation:
   def apply(fireStation: FireStation, state: ZoneState): FireStation =
      FireStation(fireStation.position, fireStation.zoneId, state)
