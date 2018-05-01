/*
 * Beangle, Agile Development Scaffold and Toolkits.
 *
 * Copyright © 2005, The Beangle Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
