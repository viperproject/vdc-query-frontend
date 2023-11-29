package queryFrontend

import java.sql.Timestamp
import java.time.LocalDateTime
import ujson.Obj
import upickle.default.{write, read}
import JSONReadWriters._
import Config._

object APIQueries {

  def getProgramEntriesByMetaData(
    earliestDate: Timestamp = Timestamp.valueOf(LocalDateTime.MIN),
    latestDate: Timestamp = Timestamp.valueOf(LocalDateTime.MAX),
    minLOC: Int = 0,
    maxLOC: Int = Int.MaxValue,
    frontend: Option[String],
    verifier: Option[String],
    parseSuccess: Option[Boolean]
  ): Seq[ProgramEntry] = {
    val queryObj = Obj(
      "earliestDate" -> write(earliestDate),
      "latestDate"   -> write(latestDate),
      "minLOC"       -> write(minLOC),
      "maxLOC"       -> write(maxLOC),
      "frontend"     -> write(frontend),
      "verifier"     -> write(verifier),
      "parseSuccess" -> write(parseSuccess)
    )
    val response = requests.post(API_HOST + "/program-entries-by-meta-data", data = queryObj)
    val entries =
      try {
        read[Seq[ProgramEntry]](response.data.toString())
      } catch {
        case e: Exception => e.printStackTrace(); Seq()
      }
    entries
  }

  def getProgramIdsByFeatureValue(feature: String, value: String): Seq[Long] = {
    val response = requests.get(s"$API_HOST/program-entries-by-meta-data?feature=$feature&value=$value")
    val ids =
      try {
        read[Seq[Long]](response.data.toString())
      } catch {
        case e: Exception => e.printStackTrace(); Seq()
      }
    ids
  }

  def getSiliconResultsByIds(entryIds: Seq[Long]): Seq[VerResult] = {
    val queryObj = Obj(
      "entryIds" -> write(entryIds)
    )
    val response = requests.post(s"$API_HOST/silicon-results-by-ids", data = queryObj)
    val res =
      try {
        read[Seq[VerResult]](response.data.toString())
      } catch {
        case e: Exception => e.printStackTrace(); Seq()
      }
    res
  }

  def getCarbonResultsByIds(entryIds: Seq[Long]): Seq[VerResult] = {
    val queryObj = Obj(
      "entryIds" -> write(entryIds)
    )
    val response = requests.post(s"$API_HOST/carbon-results-by-ids", data = queryObj)
    val res =
      try {
        read[Seq[VerResult]](response.data.toString())
      } catch {
        case e: Exception => e.printStackTrace(); Seq()
      }
    res
  }

  def getFrontendCountByIds(entryIds: Seq[Long]): Seq[(String, Int)] = {
    val queryObj = Obj(
      "entryIds" -> write(entryIds)
    )
    val response = requests.post(s"$API_HOST/frontend-count-by-ids", data = queryObj)
    val counts =
      try {
        read[Seq[(String, Int)]](response.data.toString())
      } catch {
        case e: Exception => e.printStackTrace(); Seq()
      }
    counts
  }

  def getRegexMatchResultsDetailed(regex: String): Seq[PatternMatchResult] = {
    val queryObj = Obj(
      "regex" -> write(regex)
    )
    val response = requests.post(s"$API_HOST/match-regex-detailed", data = queryObj)
    val matches =
      try {
        read[Seq[PatternMatchResult]](response.data.toString())
      } catch {
        case e: Exception => e.printStackTrace(); Seq()
      }
    matches
  }

  def getRegexMatchResults(regex: String): Seq[Long] = {
    val queryObj = Obj(
      "regex" -> write(regex)
    )
    val response = requests.post(s"$API_HOST/match-regex", data = queryObj)
    val matches =
      try {
        read[Seq[Long]](response.data.toString())
      } catch {
        case e: Exception => e.printStackTrace(); Seq()
      }
    matches
  }

}
