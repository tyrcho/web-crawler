package info.daviot.scraper

import scala.concurrent.duration._
import scala.concurrent.Future
import grizzled.slf4j.Logging
import dispatch._
import Defaults._
import com.ning.http.client.extra.ThrottleRequestFilter

class DispatchUrlReader extends Reader[String, String] with Logging {
  def read(urlStr: String): Future[String] = {
    debug(s"reading from $urlStr")

    val svc = url(urlStr)

    val client = Http
      .configure(_.addRequestFilter(new ThrottleRequestFilter(1)))
      .configure(_.setFollowRedirects(true))

    val read = () => client(svc.OK(as.String)).option

    //see http://www.bimeanalytics.com/engineering-blog/retrying-http-request-in-scala/
    for (data <- retry.Backoff()(read))
      yield data.getOrElse(s"ERROR reading $urlStr")
  }
}