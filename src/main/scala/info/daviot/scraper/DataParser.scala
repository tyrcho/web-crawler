package info.daviot.scraper

import scala.concurrent.Future

trait DataParser[Id, Data] {
  def extract(id: Id, content: String): Future[Option[Data]]
}