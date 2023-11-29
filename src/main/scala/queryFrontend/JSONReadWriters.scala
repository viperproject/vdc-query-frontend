package queryFrontend

import upickle.default.{ReadWriter => RW, macroRW}
import java.sql.Timestamp

/** Upickle read writers for serializable objects */
object JSONReadWriters {
  implicit val peRW: RW[ProgramEntry] = macroRW
  implicit val tsRW: RW[Timestamp] = upickle.default
    .readwriter[String]
    .bimap[Timestamp](
      t => s"${t.getTime}",
      str => new Timestamp(str.toLong)
    )
  implicit val veRW: RW[VerError]            = macroRW
  implicit val vrRW: RW[VerResult]           = macroRW
  implicit val pmrRW: RW[PatternMatchResult] = macroRW
}
