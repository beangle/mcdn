package org.beangle.micdn.service

import java.net.URL
import org.beangle.commons.lang.Strings

/**
 * Content Repository
 * /path->loader
 */
class Repository(repos: List[SubRepository]) {

  def get(path: String): Option[URL] = {
    repos.find(x => path.startsWith(x.prefix)) match {
      case None       => None
      case Some(repo) => repo.get(path)
    }
  }
}