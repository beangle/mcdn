package org.beangle.micdn.service

import java.net.URL
import java.io.File
import org.beangle.commons.io.Dirs

class FileContentLoader(dir: File) extends ContentLoader {

  require(dir.exists(), s"${dir.getAbsolutePath} doesn't exists.")

  override def load(path: String): Option[URL] = {
    val target = new File(dir.getAbsolutePath + path)
    if (target.exists()) {
      Some(target.toURI.toURL)
    } else {
      None
    }
  }
}