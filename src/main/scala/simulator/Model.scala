package simulator

import simulator.Body.{FRICTION_K, REPULSIVE_K, distance}

import scala.annotation.targetName
import scala.collection.mutable
import scala.util.Random



case class Simulation(numBodies: Int, var iteration: Int):
  val boundary: Boundary = Boundary(-6.0, -6.0, 6.0, 6.0)
  var bodies: mutable.Seq[Body] = this.generateGameObject()
  private def generateGameObject(): mutable.Seq[Body] =
    var myBodies: mutable.Seq[Body] = mutable.Seq.empty
    for (id <- 0 until numBodies)
      val x = boundary.x0 * 0.25 + Random.nextDouble() * (boundary.x1 - boundary.x0) * 0.25
      val y = boundary.y0 * 0.25 + Random.nextDouble() * (boundary.y1 - boundary.y0) * 0.25
      val body = Body(id, 10, Point2D(x, y), Point2D(0, 0)) //id, mass, position, velocity
      myBodies = myBodies :+ body
    myBodies

case class Point2D(x: Double, y: Double):
  def sum(p: Point2D): Point2D = Point2D(x + p.x, y + p.y)
  def scalarMul(s: Double): Point2D = Point2D(x * s, y * s)
  def normalize: Point2D = Math.sqrt(x*x + y*y) match
    case mod if mod > 0 => Point2D(x / mod, y / mod)

case class Boundary(x0: Double, y0: Double, x1: Double, y1: Double)

case class Body(id: Int, mass: Double, var pos: Point2D, var vel: Point2D):
  def updatePos(dt: Double): Point2D = this.pos sum (this.vel scalarMul dt)
  def updateVelocity(acc: Point2D, dt: Double): Point2D = this.vel sum (acc scalarMul dt)
  def frictionForce: Point2D = vel scalarMul (-FRICTION_K)
  def repulsiveForceBy(b: Body): Point2D = distance(this)(b) match
    case dist if dist > 0 => Point2D(pos.x - b.pos.x, pos.y - b.pos.y)
      .normalize
      .scalarMul(b.mass * REPULSIVE_K / (dist*dist))
  def checkAndSolveBoundaryCollision(bounds: Boundary): Unit =
    if pos.x > bounds.x1 then
      this.pos = Point2D(bounds.x1, pos.y)
      this.vel = Point2D(-vel.x, vel.y);
    else if pos.x < bounds.x0 then
      this.pos = Point2D(bounds.x0, pos.y)
      this.vel = Point2D(-vel.x, vel.y);

    if pos.y > bounds.y1 then
      this.pos = Point2D(pos.x, bounds.y1)
      this.vel = Point2D(vel.x, -vel.y);
    else if pos.y < bounds.y0 then
      this.pos = Point2D(pos.x, bounds.y0)
      this.vel = Point2D(vel.x, -vel.y);

object Body:
  val REPULSIVE_K = 0.01
  val FRICTION_K = 1
  def distance(b1: Body)(b2: Body): Double =
    val dx = b1.pos.x - b2.pos.x
    val dy = b1.pos.y - b2.pos.y
    Math.sqrt(dx*dx + dy*dy)


