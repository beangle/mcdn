package org.beangle.micdn.service

import java.net.URL

class SubRepository(val prefix: String, val loaders: List[ContentLoader]) {

  def get(path: String): Option[URL] = {
    var result: Option[URL] = null
    loaders.find { l =>
      result = l.load(path)
      result.isDefined
    }
    result
  }
}