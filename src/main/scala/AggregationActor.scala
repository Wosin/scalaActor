import akka.actor.{ActorLogging, Cancellable}
import akka.persistence.{AtLeastOnceDelivery, PersistentActor, SnapshotOffer}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}
case class SessionData(shopToken: String, sizeInBytes: Long) {
}

case class BufferState(messages: Seq[SessionData] = Seq.empty, totalSizeInBytes: Long = 0) {
  def newMessage(sessionData: SessionData): BufferState = copy(messages :+ sessionData, totalSizeInBytes + sessionData.sizeInBytes)
  def isBufferFull: Boolean = messages.size >= 10000 || totalSizeInBytes >= 20000000
}

trait MessageSender {
  def send(messages: Seq[SessionData]): Unit
}

case class NewBufferEntry (data: SessionData)

case class BufferFlushed (data: String)

case object FlushBuffer

trait MessageTransformer {
  def transformMessage(message: SessionData): Future[SessionData]
}

trait AggregationActor extends PersistentActor with MessageSender with MessageTransformer with ActorLogging {
  var bufferState: BufferState = BufferState()

  def scheduleCleanup(): Cancellable = context.system.scheduler.scheduleOnce(10 minutes){self ! FlushBuffer }

  var scheduledCleanup: Cancellable = scheduleCleanup()

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  def uptadeState(sessionData: Future[SessionData]) = {
    sessionData.onComplete({
      case Success(data) => {
        bufferState = bufferState.newMessage(data)
        if(bufferState.isBufferFull){
          self ! FlushBuffer
        }
      }
      case Failure(reason) => log.error("Failed to transform message! Reason:", reason)
    })

    scheduledCleanup.cancel()
    scheduledCleanup = scheduleCleanup()
  }

  def flushBuffer() = {
    send(bufferState.messages)
    bufferState = BufferState()
  }

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, snapshot: BufferState) => bufferState = snapshot
  }

  override def receiveCommand: Receive = {
    case sessionData: SessionData => persist(NewBufferEntry(sessionData))(event => uptadeState(transformMessage(event.data)) )
    case FlushBuffer => persist(BufferFlushed)(event => flushBuffer())
  }
}
