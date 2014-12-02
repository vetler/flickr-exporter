package net.roeim.flickrexporter

import java.io._
import java.net.{HttpURLConnection, URL}
import java.util.Scanner

import com.flickr4java.flickr.auth.Permission
import com.flickr4java.flickr.people.User
import com.flickr4java.flickr.photosets.Photoset
import com.flickr4java.flickr.util.FileAuthStore
import com.flickr4java.flickr.{Flickr, REST, RequestContext}
import org.scribe.model.Verifier

import scala.collection.JavaConversions._

case class Config(apiKey: String = "", sharedSecret: String = "", username: String = "", command: String = "", id: String = "")

class FlickrExporter(config: Config) {

  /**
   * Stores authorization tokens, so you don't have to authorize every time
   */
  val fileAuthStore: FileAuthStore = new FileAuthStore(new File(System.getProperty("user.home") + File.separator + ".flickrAuthStore"))

  /**
   * From a username, look up the user ID
   * @param flickr
   * @param username
   * @return
   */
  def getUserId(flickr: Flickr, username: String) = {
    val user: User = flickr.getPeopleInterface.findByUsername(username)
    user.getId
  }


  /**
   * If the authorization information is cached, return it. If not, tell the user to go to Flickr and log in.
   * @return
   */
  def authorize = {
    Option(fileAuthStore.retrieve(userId)).getOrElse({
      val authInterface = flickr.getAuthInterface()
      val scanner = new Scanner(System.in);
      val token = authInterface.getRequestToken();
      val url = authInterface.getAuthorizationUrl(token, Permission.READ)
      println("Follow this URL to authorize yourself on Flickr")
      println(url)
      println("Paste in the token it gives you:")
      print(">>")

      val tokenKey = scanner.nextLine()
      scanner.close()

      val requestToken = authInterface.getAccessToken(token, new Verifier(tokenKey))
      val auth = authInterface.checkToken(requestToken)
      println("Authentication success")
      fileAuthStore.store(auth)
      auth
    })
  }

  /**
   * Download a URL to a file in a directory
   * @param directory
   * @param url
   */
  def downloadFile(directory: String, url: String) {
    val in = GET(new URL(url)).getInputStream
    val out = new BufferedOutputStream(
      new FileOutputStream(directory + File.separator + url.substring(url.lastIndexOf("/") + 1)))
    val byteArray = Stream.continually(in.read()).takeWhile(-1 !=).map(_.toByte).toArray
    out.write(byteArray)
    out.close
  }

  def GET(url: URL): HttpURLConnection = {
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    connection
  }

  /**
   * Look up a photoset and download each photo into a directory named after the photoset title
   */
  def downloadPhotoset {
    val photoset = setsInterface.getInfo(config.id)
    println(s"Downloading photoset ${photoset.getId} ${photoset.getTitle}")
    val photos = setsInterface.getPhotos(config.id, 500, 1)

    val directory = new File(photoset.getTitle)
    directory.mkdir()

    photos.foreach(photo => {
      downloadFile(directory.getName, photo.getOriginalUrl)
    })

    println("Done!")
  }

  /**
   * List photosets belonging to the user
   */
  def listPhotosets {
    flickr.getPhotosetsInterface.getList(userId).getPhotosets foreach printPhotosetInfo
  }

  def printPhotosetInfo(photoset: Photoset) {
    println(s"${photoset.getId} ${photoset.getTitle}")
  }

  val flickr = new Flickr(config.apiKey, config.sharedSecret, new REST());
  val userId = getUserId(flickr, config.username)
  val collectionsInterface = flickr.getCollectionsInterface
  val setsInterface = flickr.getPhotosetsInterface

  val auth = authorize

  RequestContext.getRequestContext().setAuth(auth);
  flickr.setAuth(auth)

  config.command match {
    case "list-photosets" => listPhotosets
    case "download-photosets" => downloadPhotoset
  }
}


object FlickrExporter {
  def main(args: Array[String]) {
    val parser = new scopt.OptionParser[Config]("FlickrExporter") {
      head("FlickrExporter", "1.0")
      opt[String]('a', "api-key") required() action { (value, config) =>
        config.copy(apiKey = value)
      } text ("api key is required")
      opt[String]('s', "shared-secret") required() action { (value, config) =>
        config.copy(sharedSecret = value)
      } text ("shared secret is required")
      opt[String]('u', "username") required() action { (value, config) =>
        config.copy(username = value)
      } text ("username is required")

      cmd("list-photosets") action { (_, config) =>
        config.copy(command = "list-photosets")
      } text ("list available sets")
      cmd("download-photoset") action { (_, config) =>
        config.copy(command = "download-photosets")
      } text ("download a photo set") children (
        opt[String]("id") required() action { (id, config) =>
          config.copy(id = id)
        } text ("photoset ID"))
      checkConfig { config =>
        if (config.command == "")
          failure("no command specified")
        else success
      }
    }

    parser.parse(args, Config()) map { config =>
      new FlickrExporter(config)
    } getOrElse {
      // arguments are bad, error message will have been displayed
    }
  }

}
