package net.datapusher.twitterutil

import scala.collection.JavaConversions._
import twitter4j._

/**
 * Scala wrapper functions for Twitter4j.
 *
 * These abstract over iterative calls to the API to fetch more elements of a collection, allowing the caller
 * to work on a single lazy stream and not worry about book-keeping around cursors etc.
 *
 * So far these wrappers are simplistic; allowing only lookups on username and not user ID or others. There is
 * no handling of call throttling, so extensive usage may throw an exception.
 *
 * @param twitter the backing Twitter object
 */
class TwitterUtil(twitter: Twitter = TwitterFactory.getSingleton) {

  /**
   * Wraps an API call using pagination into a Stream.
   * @param username the username to look up for
   * @param moref iteration function
   * @tparam A the type of TwitterResponse to return
   */
  def paged[A <: TwitterResponse](username: String, moref: (String, Paging) => ResponseList[A]): Stream[A] = {
    def fetch(paging: Paging, things: Iterable[A], last: Boolean): Stream[A] = things match {
      case Nil if last => Stream.empty
      case Nil =>
        val more = moref(username, paging)
        if (more.isEmpty)
          Stream.empty
        else {
          val isLast = more.size() < paging.getCount
          val nextPage = new Paging(paging.getPage + 1, paging.getCount)
          fetch(nextPage, more.toList, isLast)
        }
      case thing :: rest => thing #:: fetch(paging, rest, last)
    }
    fetch(new Paging(1, 20), List.empty, last = false)
  }

  /**
   * Wraps an API call using cursors into a Stream.
   * @param username the username to look up for
   * @param moref iteration function
   * @tparam A the type of TwitterResponse to return
   */
  def cursored[A <: TwitterResponse](username: String, moref: (String, Long) => PagableResponseList[A]): Stream[A] = {
    def fetch(cursor: Long, things: Iterable[A]): Stream[A] = things match {
      case Nil if cursor == 0 => Stream.empty
      case Nil =>
        val more = moref(username, cursor)
        fetch(more.getNextCursor, more.toList)
      case thing :: rest => thing #:: fetch(cursor, rest)
    }
    fetch(-1, List.empty)
  }

  def followers(username: String): Stream[User] = cursored(username, twitter.getFollowersList)
  def friends(username: String): Stream[User] = cursored(username, twitter.getFriendsList)
  def timeline(username: String): Stream[Status] = paged(username, twitter.getUserTimeline)

}
