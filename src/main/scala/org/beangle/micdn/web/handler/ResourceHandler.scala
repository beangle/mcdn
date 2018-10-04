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
package org.beangle.micdn.web.handler

import java.io.File
import java.net.URL

import org.beangle.commons.activation.MimeTypes
import org.beangle.commons.lang.Strings
import org.beangle.commons.logging.Logging
import org.beangle.commons.web.io.DefaultWagon
import org.beangle.commons.web.util.RequestUtils
import org.beangle.micdn.service.{ PathUtils, Repository, RepositoryBuilder }
import org.beangle.webmvc.api.util.CacheControl
import org.beangle.webmvc.execution.Handler

import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
import org.beangle.commons.lang.ClassLoaders
import org.beangle.commons.io.IOs

object ResouceHander extends Logging {

  val repository: Repository = {
    val configFile = System.getProperty("beangle.micdn.config")
    if (null == configFile) {
      logger.warn("Using -Dbeangle.micdn.config=/path/to/cdn.xml to config,now using sample one.")
      RepositoryBuilder.build(ClassLoaders.getResource("sample.xml").get)
    } else {
      val file = new File(PathUtils.normalizeFilePath(configFile))
      require(file.exists(), s"${file.getAbsolutePath} doesn't exists!")
      RepositoryBuilder.build(file.toURI.toURL)
    }
  }
}

class ResourceHandler extends Handler {

  val wagon = new DefaultWagon

  val expireMinutes = 60 * 24 * 7

  def handle(request: HttpServletRequest, response: HttpServletResponse): Any = {
    val path = RequestUtils.getServletPath(request)
    find(path) match {
      case Some(url) =>
        val conn = url.openConnection()
        val lastModified = conn.getLastModified
        val ext = Strings.substringAfterLast(path, ".")
        if (Strings.isNotEmpty(ext)) {
          MimeTypes.getMimeType(ext) foreach (m => response.setContentType(m.toString))
          response.addHeader("Access-Control-Allow-Origin", "*")
        }
        if (etagChanged(String.valueOf(lastModified), request, response)) {
          CacheControl.expiresAfter(expireMinutes, response)
          response.setContentLength(conn.getContentLength)
          response.setDateHeader("Last-Modified", lastModified)
          wagon.copy(conn.getInputStream, request, response)
          response.setStatus(HttpServletResponse.SC_OK)
        }
      case None =>
        if (Strings.isEmpty(path)) {
          response.setContentType("application/xml")
          IOs.copy(ResouceHander.repository.url.openStream(), response.getOutputStream)
        } else {
          response.setStatus(HttpServletResponse.SC_NOT_FOUND)
        }
    }
  }

  /**
   * return true if etag changed
   */
  private def etagChanged(etag: String, request: HttpServletRequest, response: HttpServletResponse): Boolean = {
    val requestETag = request.getHeader("If-None-Match")
    // not modified, content is not sent - only basic headers and status SC_NOT_MODIFIED
    val changed = !etag.equals(requestETag)
    if (!changed) response.setStatus(HttpServletResponse.SC_NOT_MODIFIED)
    else response.setHeader("ETag", etag)
    changed
  }

  private def find(path: String): Option[URL] = {
    ResouceHander.repository.get(path)
  }

}
