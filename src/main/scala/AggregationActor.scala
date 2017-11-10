import akka.actor.{Cancellable, Scheduler}
import akka.persistence.PersistentActor

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

abstract case class SessionData(shopToken: String) {
  def sizeInBytes: Long
}

case class BufferState(messages: Seq[SessionData] = Seq.empty, totalSizeInBytes: Long = 0) {
  def newMessage(sessionData: SessionData): BufferState = copy(messages :+ sessionData, totalSizeInBytes + sessionData.sizeInBytes)
  def isBufferFull: Boolean = messages.size >= 10000 || totalSizeInBytes >= 20000000
}

trait MessageSender {
  def send(messages: Seq[SessionData]): Unit
}

trait MessageTransformer {
  def transformMessage(message: SessionData): Future[SessionData]
}

abstract class AggregationActor(id: String) extends PersistentActor with MessageSender with MessageTransformer with Scheduler {
  var bufferState: BufferState = BufferState()

  def scheduleCleanup: Cancellable = scheduleOnce(10 minutes){flushBuffer}

  var scheduledCleanup: Cancellable = scheduleCleanup

  override def persistenceId: String = id

  def uptadeState(sessionData: SessionData) = {
    bufferState = bufferState.newMessage(sessionData)
    if(bufferState.isBufferFull) {
      send(bufferState.messages)
    }
    scheduledCleanup.cancel()
    scheduledCleanup = scheduleCleanup
  }

  def flushBuffer = {
    send(bufferState.messages)
    bufferState = BufferState()
  }
  override def receiveCommand: Receive = {
    case sessionData: SessionData => uptadeState(sessionData)
  }
}
