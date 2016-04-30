package info.daviot.soup.hearthstone

import info.daviot.scraper.Scraper
import scala.concurrent.Future
import info.daviot.scraper.DispatchUrlReader
import org.jsoup.nodes.Document
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import info.daviot.soup.DispatchJsoupScraper
import info.daviot.scraper.DataParser
import info.daviot.scraper.LinksParser
import info.daviot.soup.JsoupParser
import org.jsoup.select.Elements

class HsTopDecksScraper(
  val initial: Iterable[String],
  cacheFolder: String,
  seasons: Int*)
    extends DispatchJsoupScraper[String](
      HstdDataParser,
      new HstdLinksParser(seasons: _*),
      cacheFolder)

object HstdDataParser extends DataParser[String, String] with JsoupParser {
  def parseCards(content: Element) = for {
    a <- content.select("a")
    name = a.select("span.name").head.text
    count = a.select("span.count").headOption.map(_.text.toInt).getOrElse(1)
  } yield s"${count}x $name"

  private def parseCards(data: Elements) = for {
    cardLine <- data
    name = cardLine.select(".card-name").head.text
    count = cardLine.select(".card-count").head.text.toInt
  } yield s"$count $name"

  def extract(id: String, content: String): Future[Option[String]] = for {
    doc <- parseDocument(content)
  } yield for {
    cl <- doc.select("div.decks-info").headOption
    c = cl.select("a").head.text
    cards = doc.select("a.card-frame")
  } yield s"${doc.title} $c,${parseCards(cards).toList} $id"
}

class HstdLinksParser(seasons: Int*) extends LinksParser[String] with JsoupParser {
  def extract(id: String, content: String): Future[List[String]] =
    for {
      doc <- parseDocument(content)
      seasonStrings = seasons.map(s => s"http://www.hearthstonetopdecks.com/deck-category/.*/season-$s/page/").mkString("|")
    } yield (for {
      link <- doc.getElementsByAttributeValueMatching(
        "href", s"(http://www.hearthstonetopdecks.com/decks/|$seasonStrings)").toList
      if !id.contains("/decks/") // do not follow links from Deck page
    } yield link.attr("href")).distinct
}
 