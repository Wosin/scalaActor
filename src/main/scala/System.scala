import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.ShardRegion.{ExtractEntityId, ExtractShardId, MessageExtractor}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}

object System {
  def main(args: Array[String]): Unit = {

    val system = ActorSystem("akanoo-system")

    val actorRegion: ActorRef = ClusterSharding(system).start(
      typeName = "AggregationActor",
      entityProps = Props[TestActor],
      settings = ClusterShardingSettings(system),
      extractEntityId = entityIdextractor,
      extractShardId = shardIdExtractor
    )

    SharedLeveldbJournal.setStore(system.actorOf(Props(new SharedLeveldbStore())), system)
  }

  val entityIdextractor: ExtractEntityId = {
    case sessionData@SessionData(shopToken, _) => (shopToken.toString, sessionData)
  }

  val shardIdExtractor: ExtractShardId = {
    case sessionData@SessionData(shopToken, _) => shopToken.toString
  }
}