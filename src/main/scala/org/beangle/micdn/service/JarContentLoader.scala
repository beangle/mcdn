package org.beangle.micdn.service

import java.net.{ URL, URLClassLoader }

object JarContentLoader {
  def apply(location: String, jars: List[URL]): JarContentLoader = {
    require(jars.size > 0, "jars should not empty")
    new JarContentLoader(PathUtils.trimLastSlash(location), jars)
  }

  def webjars(jars: List[URL]): JarContentLoader = {
    apply("META-INF/resources/webjars", jars)
  }

  def s3jars(jars: List[URL]): JarContentLoader = {
    apply("META-INF/resources", jars)
  }
}

class JarContentLoader(val location: String, val jars: List[URL]) extends ContentLoader {

  val loader = new URLClassLoader(jars.toArray)

  override def load(path: String): Option[URL] = {
    Option(loader.getResource(location + path))
  }

}