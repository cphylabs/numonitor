package com.olchovy.numonitor.actor

import scala.collection.immutable.Queue
import akka.actor._
import akka.util._

import ExpiringBoundedQueue._

/**
 * A queue with a maximum size of capacity.
 *
 * The queue will be flushed when its size reaches capacity or, iff non-empty, when the expiry time is reached.
 */
abstract class ExpiringBoundedQueue[A : Manifest](capacity: Int, expiry: FiniteDuration) extends Actor with FSM[State, Queue[A]]
{
  import FSM._

  def size = stateData.size

  /* A user-defined function that will be called when the queue is to be flushed
   * @see ExpiringBoundedQueue (class definition)
   */
  protected def onFlush: Queue[A] => Unit

  /* Is the given item queueable? */
  private def isQueueable[B : Manifest](a: Any) = manifest[A] >:> implicitly[Manifest[B]]

  private def empty: Queue[A] = Queue.empty[A]

  /* A convenience handler for flushing all items from the queue and returning the FSM to an Empty state */
  private def flush(queue: Queue[A]) = {
    onFlush(queue)
    goto(Empty).using(empty)
  }

  /* A convenience handler for the reception of Stop events--the endgame for the FSM */
  private def terminate(queue: Queue[A]) = {
    onFlush(queue)
    stop
  }

  override def toString = "ExpiringBoundedQueue(capacity=%d, expiry=%s, size=%s)".format(capacity, expiry, size)

  startWith(Empty, empty)

  when(Empty) {
    case Event(Enqueue(a), queue) if isQueueable(a) => goto(NonEmpty).using(queue.enqueue(a.asInstanceOf[A]))
    case Event(Dequeue | Flush, _) => stay
    case Event(Stop, _) => stop
  }

  when(NonEmpty, stateTimeout = expiry) {
    case Event(StateTimeout, queue) => flush(queue)
    case Event(Enqueue(a), queue) if isQueueable(a) && queue.size == capacity - 1 => flush(queue.enqueue(a.asInstanceOf[A]))
    case Event(Enqueue(a), queue) if isQueueable(a) => stay.using(queue.enqueue(a.asInstanceOf[A]))
    case Event(Dequeue, queue) if queue.size == 1 => goto(Empty).using(empty)
    case Event(Flush, queue) => flush(queue)
    case Event(Stop, queue) => terminate(queue)
  }

  whenUnhandled {
    case Event(e, _) =>
      //logger.warn("Unhandled event: %s".format(e))
      println("Unhandled event: %s".format(e))
      stay
  }

  initialize
}

object ExpiringBoundedQueue
{
  sealed trait State

  private case object Empty extends State

  private case object NonEmpty extends State

  case class Enqueue[A](a: A)

  case object Dequeue

  case object Flush

  case object Stop

  def apply[A : Manifest](capacity: Int, expiryInMilliseconds: Long)(f: Queue[A] => Unit): ActorRef = {
    val expiry = new FiniteDuration(expiryInMilliseconds, "milliseconds")
    val actor = Actor.actorOf(new ExpiringBoundedQueue[A](capacity, expiry) { protected def onFlush = f })
    actor.start
  }
}
