package it.unibo.pcd.distributed.behavior

import it.unibo.pcd.distributed.model.FireStation

enum StateRequestMessage[T] extends Message:
  case ResponseState(t: T)