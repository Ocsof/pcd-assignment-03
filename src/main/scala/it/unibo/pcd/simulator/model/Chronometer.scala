package it.unibo.pcd.simulator.model

object Chronometer {
  var running: Boolean = false
  var startTime: Long = 0

  def start(): Unit =
    running = true
    startTime = System.currentTimeMillis()

  def stop(): Unit =
    startTime = time
    running = false

  def time: Long =
    if running then System.currentTimeMillis() - startTime else startTime
}
