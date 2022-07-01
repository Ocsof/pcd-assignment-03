package it.unibo.pcd.distributed.behavior

import akka.actor.typed.ActorRef

case class StateResponse[T](t: T) extends Message