import java.time._
import java.time.format._

name := "tgfd"
version := OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy.MM.dd.hhmm"))
scalaVersion := "2.11.12"