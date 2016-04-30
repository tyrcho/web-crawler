package info.daviot.scraper

import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait AsyncCache[A, B] {
  def withCache(f: A => Future[B]): A => Future[B] = {
    def getPut(in: A, maybe: Option[B]): Future[B] = maybe match {
      case Some(out) => Future.successful(out)
      case None =>
        for { out <- f(in) } yield {
          put(in, out)
          out
        }
    }

    (in: A) => (for {
      maybe <- get(in)
      res <- getPut(in, maybe)
    } yield res)
  }

  def get(in: A): Future[Option[B]]

  def put(in: A, out: B): Future[Unit]
}

class FileCache(folderStr: String) extends AsyncCache[String, String] {
  def get(in: String) = Future {
    val file = cacheFile(in)
    if (file.exists) {
      Some(io.Source.fromFile(file).mkString)
    } else None
  }

  def put(in: String, content: String) = Future {
    val w = new FileWriter(cacheFile(in))
    w.write(content)
    w.close()
  }

  def cacheFile(in: String) = {
    val parts = in.replaceAll("\\?|&|:", "/").split("/").drop(2)
    val folder = new File(folderStr, parts.init.mkString("/"))
    folder.mkdirs()
    new File(folder, parts.last + ".html")
  }
}

