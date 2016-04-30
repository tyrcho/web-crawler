package info.daviot.scraper

import scala.concurrent.Future

trait LinksParser[Id] {
  def extract(id: Id, content: String): Future[List[Id]]
}