package it.unibo.pcd.distributed

import it.unibo.pcd.distributed.model.*

import scala.util.Random
import com.typesafe.config.{Config, ConfigFactory}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import it.unibo.pcd.distributed.model.ZoneState.Free
import it.unibo.pcd.distributed.behavior.{FireStationActor, FireStationGuardian, PluviometerActor, PluviometerGuardian, Update, ViewActor, ViewGuardian}
import it.unibo.pcd.distributed.model.FireStation


object App {

  def generateZones(rows: Int, columns: Int, zoneBoundary: Boundary): List[Zone] =
    var zones: List[Zone] = List()
    var zoneId: Int = 0
    for i <- 0 until columns do
      for j <- 0 until rows do
        zones = Zone(zoneId,
                     Boundary(i * zoneBoundary.width, j * zoneBoundary.height,
                             (i * zoneBoundary.width) + zoneBoundary.width - 1,
                             (j * zoneBoundary.height) + zoneBoundary.height - 1)) :: zones
        zoneId = zoneId + 1
    zones.reverse

  def generatePluviometers(zones: List[Zone]): List[Pluviometer] =
    var pluviometers: List[Pluviometer] = List()
    val random: Random = Random(System.currentTimeMillis())
    var pluviometerId: Int = 0
    var zoneID = 0
    zones.foreach(x => {
      pluviometers = Pluviometer(zoneID,
        Point2D(random.nextInt(x.width - 100) + x.boundary.x0 + 50, random.nextInt(x.height - 100) + x.boundary.y0 + 50),
        5) :: pluviometers
      zoneID = zoneID + 1
      pluviometerId = pluviometerId + 1
    })
    pluviometers

  def generateFireStations(zones: List[Zone]): List[FireStation] =
    var fireStations: List[FireStation] = List()
    val random: Random = Random(System.currentTimeMillis())
    var zoneID = 0
    zones.foreach(x => {
        fireStations = FireStation(
           Point2D(random.nextInt(x.width - 100) + x.boundary.x0 + 50, random.nextInt(x.height - 100) + x.boundary.y0 + 50) ,
           zoneID,
          ZoneState.Free) :: fireStations
        zoneID = zoneID + 1
    })
    fireStations.reverse


  @main
  def main(): Unit = {
    val rows = 2
    val columns = 2
    val width = columns * 200
    val height = rows * 200
    var port: Int = 25551
    val zones: List[Zone] = generateZones(rows, columns, Boundary(0, 0, width / columns, height / rows))
    val pluviometers: List[Pluviometer] = generatePluviometers(zones)
    val fireStations: List[FireStation] = generateFireStations(zones.slice(0, 3))

    pluviometers.foreach(pluviometer => {
      startup(port = port)(PluviometerGuardian(pluviometer))
      port = port + 1
    })
    println(pluviometers.size.toString)

    fireStations.foreach(fireStation => {
      startup(port = port)(FireStationGuardian(fireStation))
      port = port + 1
    })
    println(fireStations.size.toString)
    startup(port = 1200)(ViewGuardian(width, height, zones))
    startup(port = 1201)(ViewGuardian(width, height, zones))
  }

  def startup[X](file: String = "cluster", port: Int)(root: => Behavior[X]): ActorSystem[X] =
    // Override the configuration of the port
    val config: Config = ConfigFactory
      .parseString(s"""akka.remote.artery.canonical.port=$port""")
      .withFallback(ConfigFactory.load(file))
    // Create an Akka system
    ActorSystem(root, "ClusterSystem", config)
}
