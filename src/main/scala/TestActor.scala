import scala.concurrent.Future
class TestActor extends AggregationActor {
  override def send(messages: Seq[SessionData]): Unit = print(messages)

  override def transformMessage(message: SessionData): Future[SessionData] = Future.successful(message)

}
