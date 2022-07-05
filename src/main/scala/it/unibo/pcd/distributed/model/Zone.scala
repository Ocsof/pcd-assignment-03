package it.unibo.pcd.distributed.model

case class Zone(zoneId: Int, boundary: Boundary):
  def width: Int = boundary.x1 - boundary.x0
  def height: Int = boundary.y1 - boundary.y0
