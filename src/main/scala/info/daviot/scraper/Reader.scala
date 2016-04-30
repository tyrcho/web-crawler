package info.daviot.scraper

import scala.concurrent.Future

trait Reader[I,O] {
  def read(id: I): Future[O]
}