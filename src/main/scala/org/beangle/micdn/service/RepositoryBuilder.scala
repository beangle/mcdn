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

object RepositoryBuilder {
  def build(file: File): Repository = {
    require(file.exists(), s"${file.getAbsolutePath} doesn't exists!")

    val xml = scala.xml.XML.load(new FileInputStream(file))

    var downloader: ArtifactDownloader = null
    var localRepo: Repo.Local = null
    (xml \ "maven").foreach { mavenElem =>
      val remote = (mavenElem \ "@remote").text
      if (Strings.isEmpty(remote)) {
        throw new RuntimeException("maven remote url needed")
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
      val prefix = (urlElem \ "@prefix").text

      val contentLoaders = Collections.newBuffer[ContentLoader]
      val jars = Collections.newMap[String, List[URL]]

      (urlElem \ "jar") foreach { jarElem =>
        val gav = (jarElem \ "@gav").text
        val file = (jarElem \ "@file").text
        val location = (jarElem \ "@location").text
        buildJarLoader(gav, file, location, localRepo, artifacts, jars)
      }

      (urlElem \ "webjar") foreach { jarElem =>
        val gav = (jarElem \ "@gav").text
        val file = (jarElem \ "@file").text
        buildJarLoader(gav, file, "META-INF/resources/webjars", localRepo, artifacts, jars)
      }

      (urlElem \ "s3jar") foreach { jarElem =>
        val gav = (jarElem \ "@gav").text
        var file = (jarElem \ "@file").text
        buildJarLoader(gav, file, "META-INF/resources", localRepo, artifacts, jars)
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
    new Repository(subRepos.toList)
  }

  private def buildJarLoader(gav: String, file: String, location: String, localRepo: Repo.Local,
    artifacts: Buffer[Artifact], jars: collection.mutable.Map[String, List[URL]]): Unit = {
    val loc = PathUtils.trimLastSlash(location)
    var jarFile = file
    if (!Strings.isEmpty(gav)) {
      val artifact = Artifact(gav)
      artifacts += artifact
      jarFile = localRepo.url(artifact)
    }
    jarFile = PathUtils.normalizeFilePath(jarFile)
    val url = new File(jarFile).toURI.toURL
    jars.get(loc) match {
      case None       => jars.put(loc, List(url))
      case Some(urls) => jars.put(loc, url :: urls)
    }
  }
}
