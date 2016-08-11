package model

import java.time.LocalDateTime

case class Tweet(text: String, userName: String, creationTime: LocalDateTime, favorites: Int) {

}
