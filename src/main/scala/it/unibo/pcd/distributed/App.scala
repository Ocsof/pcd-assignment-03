package it.unibo.pcd.distributed

import it.unibo.pcd.distributed.model.*

import scala.util.Random
import com.typesafe.config.{Config, ConfigFactory}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import it.unibo.pcd.distributed.behavior.PluviometerActor

object App {

  def generateZones(rows: Int, columns: Int, zoneSize: Boundary): List[Boundary] =
    var zones: List[Boundary] = List()
    for i <- 0 until columns do
      for j <- 0 until rows do
        zones = Boundary(i * zoneSize.width, j * zoneSize.height, (i * zoneSize.width) + zoneSize.width - 1, (j * zoneSize.height) + zoneSize.height - 1) :: zones
    zones.reverse
/*
  def generateFireStations(zones: List[Zone]): List[FireStation] =
    var fireStations: List[FireStation] = List()
    val random: Random = Random(System.currentTimeMillis())
    zones.foreach(x => fireStations = FireStation(x.id,
      Point2D(random.nextInt(x.bounds.width - 100) + x.bounds.x0 + 50, random.nextInt(x.bounds.height - 100) + x.bounds.y0 + 50),
      Free) :: fireStations)
    fireStations.reverse

  def generatePluviometers(zones: List[Zone]): List[Pluviometer] =
    var pluviometers: List[Pluviometer] = List()
    val random: Random = Random(System.currentTimeMillis())
    val circularZones = Iterator.continually(zones).flatten.take(6)
    var pluviometerId: Int = 0
    circularZones.foreach(x => {
      pluviometers = Pluviometer(x.id,
        Point2D(random.nextInt(x.bounds.width - 100) + x.bounds.x0 + 50, random.nextInt(x.bounds.height - 100) + x.bounds.y0 + 50),
        5) :: pluviometers
      pluviometerId = pluviometerId + 1
    })
    pluviometers
*/

  @main
  def main(): Unit = {
    val rows = 2
    val columns = 2
    val width = columns * 200
    val height = rows * 200
    var port: Int = 2551
    val zones: List[Boundary] = generateZones(rows, columns, Boundary(0, 0, width / columns, height / rows))

    zones.foreach(zone => {
      startup(port = port)(PluviometerActor(zone))
      port = port + 1;
    })

  }

  def startup[X](file: String = "cluster", port: Int)(root: => Behavior[X]): ActorSystem[X] =
    // Override the configuration of the port
    val config: Config = ConfigFactory
      .parseString(s"""akka.remote.artery.canonical.port=$port""")
      .withFallback(ConfigFactory.load(file))
    // Create an Akka system
    ActorSystem(root, "ClusterSystem", config)


}
