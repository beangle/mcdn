package org.beangle.micdn.service

import java.net.URL

trait ContentLoader {

  def load(path: String): Option[URL]
}