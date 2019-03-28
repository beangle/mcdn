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

import java.io.{ File, FileInputStream }
import java.net.URL

import scala.collection.mutable.Buffer

import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings
import org.beangle.repo.artifact.{ Artifact, ArtifactDownloader, Repo }
import org.beangle.commons.lang.SystemInfo
import org.beangle.commons.logging.Logging

object RepositoryBuilder extends Logging {
  val styles = Map("webjar" -> "META-INF/resources/webjars", "s3" -> "META-INF/resources")

  def build(url: URL): Repository = {
    val xml = scala.xml.XML.load(url.openStream())

    var downloader: ArtifactDownloader = null
    var localRepo: Repo.Local = null
    (xml \ "repository").foreach { mavenElem =>
      val remote = (mavenElem \ "@remote").text
      if (Strings.isEmpty(remote)) {
        throw new RuntimeException("repository remote url needed.")
      }
      val localAttr = (mavenElem \ "@local").text
      val local = if (Strings.isEmpty(localAttr)) null else localAttr
      downloader = ArtifactDownloader(remote, local)
      localRepo = Repo.local(local)
    }

    if (null == downloader) {
      downloader = ArtifactDownloader(Repo.Remote.CentralURL, null)
      localRepo = Repo.local(null)
    }

    val artifacts = Collections.newBuffer[Artifact]

    val subRepos = Collections.newBuffer[SubRepository]
    (xml \ "contents" \ "url").foreach { urlElem =>
      var prefix = (urlElem \ "@prefix").text
      if (!prefix.endsWith("/")) prefix += "/"

      val contentLoaders = Collections.newBuffer[ContentLoader]
      val jars = Collections.newMap[String, List[URL]]

      (urlElem \ "jar") foreach { jarElem =>
        val gav = (jarElem \ "@gav").text
        if (Strings.isNotEmpty(gav)) {
          if (gav.contains("org.webjars")) {
            buildGavJarLoader(gav, "META-INF/resources/webjars", localRepo, artifacts, jars)
          } else {
            var location = (jarElem \ "@location").text
            if (Strings.isEmpty(location)) {
              location = guessStyle((jarElem \ "@style").text)
            }
            buildGavJarLoader(gav, location, localRepo, artifacts, jars)
          }
        } else {
          val file = (jarElem \ "@file").text
          var location = (jarElem \ "@location").text
          if (Strings.isEmpty(location)) {
            location = guessStyle((jarElem \ "@style").text)
          }
          buildFileJarLoader(file, location, jars)
        }
      }

      jars.foreach {
        case (location, urls) =>
          contentLoaders += JarContentLoader.apply(location, urls)
      }

      (urlElem \ "dir") foreach { dirElem =>
        val location = PathUtils.normalizeFilePath((dirElem \ "@location").text)
        contentLoaders += new FileContentLoader(new File(location))
      }
      subRepos += new SubRepository(prefix, contentLoaders.toList)
    }
    downloader.download(artifacts)
    val notexists = artifacts filter (a => !localRepo.file(a).exists())
    if (!notexists.isEmpty) {
      throw new RuntimeException(s"Cannot download these artifacts:$notexists")
    }
    new Repository(url, subRepos.toList)
  }

  private def guessStyle(name: String): String = {
    styles.get(name) match {
      case None =>
        if (!Strings.isEmpty(name)) {
          logger.warn("Cannot recogonize style :" + name)
        }
        "META-INF/resources"
      case Some(s) => s
    }
  }

  private def buildGavJarLoader(gav: String, location: String, localRepo: Repo.Local,
    artifacts: Buffer[Artifact], jars: collection.mutable.Map[String, List[URL]]): Unit = {
    val loc = PathUtils.trimLastSlash(location)
    val artifact = Artifact(gav)
    artifacts += artifact
    var jarFile = localRepo.url(artifact)
    jarFile = PathUtils.normalizeFilePath(jarFile)
    val url = new File(jarFile).toURI.toURL
    jars.get(loc) match {
      case None       => jars.put(loc, List(url))
      case Some(urls) => jars.put(loc, url :: urls)
    }
  }

  private def buildFileJarLoader(file: String, location: String,
    jars: collection.mutable.Map[String, List[URL]]): Unit = {
    val loc = PathUtils.trimLastSlash(location)
    var jarFile = file
    jarFile = PathUtils.normalizeFilePath(jarFile)
    val url = new File(jarFile).toURI.toURL
    jars.get(loc) match {
      case None       => jars.put(loc, List(url))
      case Some(urls) => jars.put(loc, url :: urls)
    }
  }
}
