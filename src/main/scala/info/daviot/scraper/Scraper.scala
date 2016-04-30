package info.daviot.scraper

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorDSL._
import akka.actor.ActorSystem
import grizzled.slf4j.Logging
import akka.actor.ActorRef
import akka.pattern.ask
import scala.concurrent.duration.DurationInt
import akka.util.Timeout
import scala.util.Success
import scala.util.Failure

abstract class Scraper[Id, Data](
  dataParser: DataParser[Id, Data],
  linksParser: LinksParser[Id],
  ioReader: Reader[Id, String],
  cache: AsyncCache[Id, String]) extends Logging {

  def read(id: Id): Future[String] =
    cache.withCache(ioReader.read)(id)

  val initial: Iterable[Id]

  implicit val timeout = Timeout(1 hour)

  def collectedData: Future[Map[Id, Data]] =
    (orchestrator ? Start(initial)).mapTo[Map[Id, Data]]

  implicit val system = ActorSystem("Scraper")

  val reader = actor(new Act {
    become {
      case Collect(id) =>
        val results = for {
          content <- read(id)
          data <- dataParser.extract(id, content)
          next <- linksParser.extract(id, content)
        } yield (data, next)
        results.onComplete {
          case Success((data, next)) =>
            for (d <- data) {
              orchestrator ! DataFound(id, d)
            }
            for (link <- next) {
              debug(s"Following link from $id to $link")
              orchestrator ! Collect(link)
            }
            orchestrator ! Done(id)
          case Failure(e) =>
            warn(s"could not process $id", e)
            orchestrator ! Done(id)
        }
    }
  })

  val orchestrator: ActorRef = actor(new Act {

    def status() {
      info(s"waiting : ${waiting.size} - done : ${data.size}")
    }

    var data = Map.empty[Id, Data]
    var waiting = Set.empty[Id]
    var done = Set.empty[Id]
    var client: ActorRef = _
    become {
      case Start(ids) =>
        data = Map.empty
        waiting = Set.empty
        done = Set.empty
        client = sender
        for (id <- ids) {
          self ! Collect(id)
        }

      case Collect(id) =>
        if (done.contains(id) || waiting.contains(id)) {
          debug(s"$id already processed or processing")
          self ! Done(id)
        } else {
          status()
          info(s"processing : $id")
          waiting += id
          reader ! Collect(id)
        }

      case DataFound(id, d) =>
        status()
        info(s"data recorded for $id")
        data += id -> d

      case Done(id) =>
        status()
        info(s"finished : $id")
        waiting -= id
        done += id
        if (waiting.isEmpty) {
          client ! data
        }
    }
  })

  case class Start(id: Iterable[Id])
  case class Collect(id: Id)
  case class Done(id: Id)
  case class DataFound(id: Id, data: Data)

}