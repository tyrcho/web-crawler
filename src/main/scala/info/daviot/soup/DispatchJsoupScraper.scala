package info.daviot.soup

import info.daviot.scraper.Scraper
import org.jsoup.Jsoup
import info.daviot.scraper.DispatchUrlReader
import scala.concurrent.Future
import org.jsoup.nodes.Document
import scala.concurrent.ExecutionContext.Implicits.global
import info.daviot.scraper.FileCache
import info.daviot.scraper.AsyncCache
import info.daviot.scraper.DispatchUrlReader
import info.daviot.scraper.DataParser
import info.daviot.scraper.LinksParser

abstract class DispatchJsoupScraper[Data](
  dataParser: DataParser[String, Data],
  linksParser: LinksParser[String],
  cacheFolder: String)
  extends Scraper[String, Data](
    dataParser,
    linksParser,
    new DispatchUrlReader, new FileCache(cacheFolder)) 