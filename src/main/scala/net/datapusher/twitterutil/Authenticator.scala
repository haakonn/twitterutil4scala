package net.datapusher.twitterutil

import twitter4j.{TwitterException, TwitterFactory}

import scala.io.StdIn

/**
 * Run this app on the command line in order to fetch an authorization token and secret.
 * Pass consumerkey and consumersecret as arguments.
 *
 * Translated to Scala from http://twitter4j.org/en/code-examples.html#oauth.
 */
object Authenticator extends App {

  val twitter = TwitterFactory.getSingleton()
  twitter.setOAuthConsumer(args(0), args(1))
  val requestToken = twitter.getOAuthRequestToken()
  println("Open this URL to grant access to your account:");
  println(requestToken.getAuthorizationURL())
  val pin = StdIn.readLine("Enter the PIN if available, or just hit enter: ")
  try {
    val accessToken = if (!pin.isEmpty)
      twitter.getOAuthAccessToken(requestToken, pin)
    else
      twitter.getOAuthAccessToken()
    println("Token: " + accessToken.getToken)
    println("Token secret: " + accessToken.getTokenSecret)
  } catch {
    case (te: TwitterException) =>
      System.out.println("Unable to get the access token.")
      te.printStackTrace()
  }

}
