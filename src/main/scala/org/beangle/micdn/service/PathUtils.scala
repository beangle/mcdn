package org.beangle.micdn.service

import org.beangle.commons.lang.SystemInfo

object PathUtils {
  def trimLastSlash(path: String): String = {
    if (path == null) ""
    else if (path.endsWith("/")) path.substring(0, path.length - 1)
    else path
  }

  def normalizeFilePath(path: String): String = {
    var file = path.trim()
    if (file.startsWith("~/")) {
      file = SystemInfo.user.home + file.substring(1)
    }
    trimLastSlash(file)
  }
}