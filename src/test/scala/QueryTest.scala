import org.scalatest.funsuite.AnyFunSuite
import queryFrontend.{APIQueries, UserSubmission}

import scala.reflect.io.File
import java.sql.Timestamp
import java.time.LocalDateTime
import scala.io.BufferedSource
import scala.io.Source.fromFile
import scala.sys.process.Process

class QueryTest extends AnyFunSuite {

  test("test all") {
    val parentRoot      = File("/Users/simon/code/viper-data-collection")
    val dbAndAPIProcess = Process(s"$parentRoot/run.sh").run
    Thread.sleep(2000) // startup
    try {
      val sampleProg = readProgram("vdc-query-frontend/src/test/resources/sample.vpr")
      val sampleUS = UserSubmission(
        0,
        Timestamp.valueOf(LocalDateTime.now()),
        "sample.vpr",
        sampleProg,
        15,
        "Silicon",
        Array("--timeout", "10"),
        "Silicon",
        true,
        1500
      )

      APIQueries.submitProgram("sample.vpr", sampleProg, "Silicon", Array("--timeout", "10"), "Silicon", true, 1500)
      Process(s"$parentRoot/bash_scripts/run_scala_class.sh dataCollection.ProcessingPipeline").!
      val peByMD = APIQueries.getProgramEntriesByMetaData()
      assert(peByMD.length == 1)
      val idsByFV = APIQueries.getProgramIdsByFeatureValue("mightHaveQP", "false")
      assert(idsByFV.length == 1)
      val silRes = APIQueries.getSiliconResultsByIds(idsByFV)
      assert(silRes.length == 1)
      val carbRes = APIQueries.getCarbonResultsByIds(idsByFV)
      assert(carbRes.length == 1)
      val feCount = APIQueries.getFrontendCountByIds(idsByFV)
      assert(feCount.length == 1)
      val patternMatchRes = APIQueries.getRegexMatchResultsDetailed("while\\([^)]*\\)")
      assert(patternMatchRes.length == 1)
      assert(patternMatchRes.head.programEntryId == idsByFV.head)
      val regMatchRes = APIQueries.getRegexMatchResults("while\\([^)]*\\)")
      assert(regMatchRes.length == 1)
      val fvs = APIQueries.getFeatureValuesByProgramId(idsByFV.head)
      println(peByMD)
      println(idsByFV)
      println(silRes)
      println(carbRes)
      println(feCount)
      println(patternMatchRes)
      println(regMatchRes)
      println(fvs)


    } finally {
      dbAndAPIProcess.destroy()
    }
  }

  def readProgram(path: String): String = {
    val fBuffer: BufferedSource = scala.io.Source.fromFile(path)
    val prog =
      try fBuffer.mkString
      finally fBuffer.close()
    prog
  }

}
