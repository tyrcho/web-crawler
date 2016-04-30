package info.daviot.soup

import org.jsoup.Jsoup
import scala.concurrent.Future
import org.jsoup.nodes.Document
import scala.concurrent.ExecutionContext.Implicits.global

trait JsoupParser {
  def parseDocument(doc: String): Future[Document] =
    Future(Jsoup.parse(doc))
}