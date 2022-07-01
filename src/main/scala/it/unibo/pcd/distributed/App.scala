package it.unibo.pcd.distributed

import it.unibo.pcd.distributed.model.*

import scala.util.Random
import com.typesafe.config.{Config, ConfigFactory}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import it.unibo.pcd.distributed.model.ZoneState.Free
import it.unibo.pcd.distributed.behavior.{FireStationActor, PluviometerActor}
import it.unibo.pcd.distributed.model.FireStation

object App {

  def generateZones(rows: Int, columns: Int, zoneSize: Zone): List[Zone] =
    var zones: List[Zone] = List()
    for i <- 0 until columns do
      for j <- 0 until rows do
        zones = Zone(i * zoneSize.width, j * zoneSize.height, (i * zoneSize.width) + zoneSize.width - 1, (j * zoneSize.height) + zoneSize.height - 1) :: zones
    zones.reverse

  def generatePluviometers(zones: List[Zone]): List[Pluviometer] =
    var pluviometers: List[Pluviometer] = List()
    val random: Random = Random(System.currentTimeMillis())
    var pluviometerId: Int = 0
    var zoneID = 0
    zones.foreach(x => {
      pluviometers = Pluviometer(zoneID,
        Point2D(random.nextInt(x.width - 100) + x.x0 + 50, random.nextInt(x.height - 100) + x.y0 + 50),
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
           Point2D(random.nextInt(x.width - 100) + x.x0 + 50, random.nextInt(x.height - 100) + x.y0 + 50) ,
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
    var port: Int = 2551
    val zones: List[Zone] = generateZones(rows, columns, Zone(0, 0, width / columns, height / rows))
    val pluviometers: List[Pluviometer] = generatePluviometers(zones)
    val fireStations: List[FireStation] = generateFireStations(zones)
    pluviometers.foreach(pluviometer => {
      startup(port = port)(PluviometerActor(pluviometer))
      port = port + 1
    })
    fireStations.foreach(fireStation => {
      startup(port = port)(FireStationActor(fireStation))
      port = port + 1
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
