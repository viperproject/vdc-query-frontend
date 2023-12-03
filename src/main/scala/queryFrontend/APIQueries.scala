package queryFrontend

import java.sql.Timestamp
import java.time.LocalDateTime
import ujson.{Arr, Obj}
import upickle.default.{read, write}
import JSONReadWriters._
import Config._

object APIQueries {

  /** Returns all programEntries from the database that match the given metadata. Default values match all programEntries.
    * If a parameter is an [[Option]] and you pass [[None]], the entries aren't filtered for that value.
    */
  def getProgramEntriesByMetaData(
    earliestDate: Timestamp = Timestamp.valueOf(LocalDateTime.of(1970, 1, 1, 1, 1)),
    latestDate: Timestamp = Timestamp.valueOf(LocalDateTime.of(2100, 1, 1, 1, 1)),
    minLOC: Int = 0,
    maxLOC: Int = Int.MaxValue,
    frontend: Option[String] = None,
    verifier: Option[String] = None,
    parseSuccess: Option[Boolean] = None
  ): Seq[ProgramEntry] = {
    val queryObj = Obj(
      "earliestDate" -> earliestDate.getTime,
      "latestDate"   -> latestDate.getTime,
      "minLOC"       -> minLOC,
      "maxLOC"       -> maxLOC,
      "frontend"     -> frontend,
      "verifier"     -> verifier,
      "parseSuccess" -> parseSuccess
    )
    val entries =
      try {
        val json = getResponseObj(s"$API_HOST/program-entries-by-meta-data", queryObj)
        read[Seq[ProgramEntry]](json("programEntries").arr)
      } catch {
        case e: Exception => e.printStackTrace(); Seq()
      }
    entries
  }

  /** Returns a list of ids for all programEntries that have a [[VerifierResult]] which produced [[feature]] with the value of [[value]] */
  def getProgramIdsByFeatureValue(feature: String, value: String): Seq[Long] = {
    val ids =
      try {
        val json = getResponseObj(s"$API_HOST/program-ids-by-feature-value?feature=$feature&value=$value", null)
        read[Seq[Long]](json("programIds").arr)
      } catch {
        case e: Exception => e.printStackTrace(); Seq()
      }
    ids
  }

  /** Returns all Silicon [[VerifierResult]]s for the given programEntryIds. This is a 1:many relationship */
  def getSiliconResultsByIds(entryIds: Seq[Long]): Seq[VerResult] = {
    val queryObj = Obj(
      "entryIds" -> entryIds
    )
    val res =
      try {
        val json = getResponseObj(s"$API_HOST/silicon-results-by-ids", queryObj)
        read[Seq[VerResult]](json("siliconResults").arr)
      } catch {
        case e: Exception => e.printStackTrace(); Seq()
      }
    res
  }

  /** Returns all Carbon [[VerifierResult]]s for the given programEntryIds. This is a 1:many relationship */
  def getCarbonResultsByIds(entryIds: Seq[Long]): Seq[VerResult] = {
    val queryObj = Obj(
      "entryIds" -> entryIds
    )
    val res =
      try {
        val json = getResponseObj(s"$API_HOST/carbon-results-by-ids", queryObj)
        read[Seq[VerResult]](json("carbonResults").arr)
      } catch {
        case e: Exception => e.printStackTrace(); Seq()
      }
    res
  }

  /** Returns a [[queryFrontend.VerVersionDifferenceSummary]] summarizing differences between SiliconResults of the two given
    * Silicon versions. Only results that are already in the database are taken into account, no new ones are generated.
    */
  def getSiliconVersionDifference(versionHash1: String, versionHash2: String): VerVersionDifferenceSummary = {
    val summary =
      try {
        val json = getResponseObj(
          s"$API_HOST/silicon-version-difference?versionHash1=$versionHash1&versionHash2=$versionHash2",
          null
        )
        read[VerVersionDifferenceSummary](json("verVersionDifferenceSummary"))
      } catch {
        case e: Exception => e.printStackTrace(); null
      }
    summary
  }

  /** Returns a [[queryFrontend.VerVersionDifferenceSummary]] summarizing differences between CarbonResults of the two given
    * Carbon versions. Only results that are already in the database are taken into account, no new ones are generated.
    */
  def getCarbonVersionDifference(versionHash1: String, versionHash2: String): VerVersionDifferenceSummary = {
    val summary =
      try {
        val json = getResponseObj(
          s"$API_HOST/carbon-version-difference?versionHash1=$versionHash1&versionHash2=$versionHash2",
          null
        )
        read[VerVersionDifferenceSummary](json("verVersionDifferenceSummary"))
      } catch {
        case e: Exception => e.printStackTrace(); null
      }
    summary
  }

  /** Returns a list of (Frontend, Count) tuples for the given programEntryIds */
  def getFrontendCountByIds(entryIds: Seq[Long]): Seq[(String, Int)] = {
    val queryObj = Obj(
      "entryIds" -> entryIds
    )
    val counts =
      try {
        val json = getResponseObj(s"$API_HOST/frontend-count-by-ids", queryObj)
        read[Seq[(String, Int)]](json("frontendCounts").arr)
      } catch {
        case e: Exception => e.printStackTrace(); Seq()
      }
    counts
  }

  /** @param regex regex string to match database programs against, should not include flags, i.e. "regex", not "/regex/gmi"
    * @param flags flags from [[java.util.regex.Pattern]] or-ed together
    *
    * UNIX_LINES = 0x01
    *
    * CASE_INSENSITIVE = 0x02
    *
    * COMMENTS = 0x04
    *
    * MULTILINE = 0x08
    *
    * LITERAL = 0x10
    *
    * DOTALL = 0x20
    *
    * UNICODE_CASE = 0x40
    *
    * CANON_EQ = 0x80
    *
    * UNICODE_CHARACTER_CLASS = 0x100
    */
  def getRegexMatchResultsDetailed(regex: String, flags: Int = 0): Seq[PatternMatchResult] = {
    val queryObj = Obj(
      "regex" -> regex,
      "flags" -> flags
    )
    val matches =
      try {
        val json = getResponseObj(s"$API_HOST/match-regex-detailed", queryObj)
        read[Seq[PatternMatchResult]](json("matchResults").arr)
      } catch {
        case e: Exception => e.printStackTrace(); Seq()
      }
    matches
  }

  /** @param regex see [[getRegexMatchResultsDetailed]]
    * @param flags see [[getRegexMatchResultsDetailed]]
    */
  def getRegexMatchResults(regex: String, flags: Int = 0): Seq[Long] = {
    val queryObj = Obj(
      "regex" -> regex,
      "flags" -> flags
    )
    val matches =
      try {
        val json = getResponseObj(s"$API_HOST/match-regex", queryObj)
        read[Seq[Long]](json("matchIds").arr)
      } catch {
        case e: Exception => e.printStackTrace(); Seq()
      }
    matches
  }

  def submitProgram(
    originalName: String,
    program: String,
    frontend: String,
    args: Array[String],
    originalVerifier: String,
    success: Boolean,
    runtime: Long
  ): Unit = {
    val submission = Obj(
      "originalName"     -> originalName,
      "program"          -> program,
      "frontend"         -> frontend,
      "args"             -> Arr.from[String](args),
      "originalVerifier" -> originalVerifier,
      "success"          -> success,
      "runtime"          -> runtime
    )
    requests.post(s"$API_HOST/submit-program", data = submission)
  }

  /** Takes an [[url]] and queries it with a GET request if [[data]] == [[null]], else a POST request with the [[data]] as the query body.
    * Returns the response body as a [[ujson.Obj]]
    */
  private def getResponseObj(url: String, data: Obj): Obj = {
    val response = if (data == null) {
      requests.get(url)
    } else {
      requests.post(url, data = data)
    }
    val json: Obj = ujson.read(response.text).obj
    json
  }

}
