package info.daviot.scraper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.DefaultRedirectStrategy
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

import grizzled.slf4j.Logging

class HttpClientUrlReader extends Reader[String, String] with Logging {
  val globalConfig = RequestConfig.custom.setCookieSpec(CookieSpecs.DEFAULT).build
  val cookieStore = new BasicCookieStore
  val httpclient =
    HttpClients.custom.setRedirectStrategy(new AllowPOSTRedirection).setDefaultRequestConfig(globalConfig).setDefaultCookieStore(cookieStore).build
  val context = HttpClientContext.create
  context.setCookieStore(cookieStore)

  def read(urlStr: String): Future[String] = Future {
    debug(s"reading from $urlStr")

    val response = httpclient.execute(new HttpGet(urlStr), context)
    EntityUtils.toString(response.getEntity)
  }
}

class AllowPOSTRedirection extends DefaultRedirectStrategy {

  override def isRedirectable(method: String) = method.equals("GET") || method.equals("POST")

}