package queryFrontend

import queryFrontend.Config.TIME_DIFFERENCE_MULTIPLIER

import java.sql.Timestamp

trait Similarity[T] {

  def isSimilarTo(other: T): Boolean

}

/** Case class to represent a row in the programs.ProgramEntries table of the database
  *
  * @param programEntryId   unique identifier for the entry
  * @param submissionDate   time when this entry was created
  * @param program          the viper program in plaintext
  * @param loc              number of lines of code
  * @param frontend         Viper frontend that produced this program
  * @param originalVerifier Verifier through which program was originally verified - Silicon or Carbon
  * @param args             the arguments originally passed to the verifier
  * @param parseSuccess     whether program was able to be parsed
  */

case class ProgramEntry(
  programEntryId: Long,
  submissionDate: Timestamp,
  program: String,
  loc: Int,
  frontend: String,
  originalVerifier: String,
  args: Array[String],
  originalRuntime: Long,
  parseSuccess: Boolean
) extends Similarity[ProgramEntry]
    with Serializable {

  /** returns whether this entry is close enough to another to count as a duplicate.
    *
    * Fields checked for equality: Frontend, Verifier, numbers of methods and functions
    *
    * Fields checked for similarity: loc, args, programPrint
    */
  def isSimilarTo(other: ProgramEntry): Boolean = {
    lazy val similarLength = this.loc <= 1.2 * other.loc && this.loc >= 0.8 * other.loc
    lazy val sameFrontend  = this.frontend == other.frontend
    lazy val sameVerifier  = this.originalVerifier == other.originalVerifier
    lazy val similarArgs   = this.args.toSet.filter(_.startsWith("--")) == other.args.toSet.filter(_.startsWith("--"))
    similarLength && sameFrontend && sameVerifier && similarArgs
  }
}

object ProgramEntry {
  def tupled = (ProgramEntry.apply _).tupled
}

/** Case class to represent a row in the programs.UserSubmissions table of the database
  *
  * @param submissionId     unique identifier for the entry
  * @param submissionDate   time when this entry was created
  * @param program          the viper program in plaintext
  * @param loc              number of lines of code
  * @param frontend         Viper frontend that produced this program
  * @param originalVerifier Verifier through which program was originally verified - Silicon or Carbon
  * @param args             the arguments originally passed to the verifier
  * @param success          whether the program verified on the users device
  * @param runtime          how long it took the user's verifier to finish
  */
case class UserSubmission(
  submissionId: Long,
  submissionDate: Timestamp,
  program: String,
  loc: Int,
  frontend: String,
  args: Array[String],
  originalVerifier: String,
  success: Boolean,
  runtime: Long
) extends Serializable

object UserSubmission {

  def tupled = (UserSubmission.apply _).tupled
}

/** Case class to represent the result of verifying a program through a Viper verifier, used for entries in
  * programs.SiliconResultTable and programs.CarbonResultTable
  *
  * @param resId          unique identifier for the entry
  * @param creationDate   time when this entry was created
  * @param verifierHash   commit hash of the verifier version used to get this result
  * @param programEntryId id referring to the ProgramEntry that was verified
  * @param success        whether program verified successfully
  * @param didTimeout     whether timeout occurred during verification
  * @param runtime        total time for verification
  * @param errors         errors encountered during verification - should be empty if [[success]]
  * @param phaseRuntimes  runtimes of the phases of the verifier
  */
case class VerResult(
  resId: Long,
  creationDate: Timestamp,
  verifierHash: String,
  programEntryId: Long,
  success: Boolean,
  didTimeout: Boolean,
  runtime: Long,
  errors: Array[VerError],
  phaseRuntimes: Array[(String, Long)]
) extends Similarity[VerResult]
    with Serializable {

  def isSimilarTo(other: VerResult): Boolean = {
    lazy val sameRes: Boolean = if (this.success) {
      other.success
    } else {
      !other.success && this.errorIdSet == other.errorIdSet
    }
    lazy val similarRuntime = similarTime(
      this.runtime,
      other.runtime
    ) // either time in +-50% of other or +-2seconds (for variance in small programs)
    similarRuntime && sameRes
  }

  def errorIdSet: Set[String] = (this.errors map (_.fullId)).toSet

  private def similarTime(t1: Long, t2: Long): Boolean = {
    ((t1 <= t2 * TIME_DIFFERENCE_MULTIPLIER && t1 >= t2 / TIME_DIFFERENCE_MULTIPLIER) || (t1 - t2).abs <= 2000)
  }
}

object VerResult {
  def tupled = (VerResult.apply _).tupled
}

/** Case class to represent a row in the programs.Features table
  *
  * @param name            name of the feature
  */
case class Feature(name: String)

/** Case class to represent a row in the programs.constFeatureEntry, programs.silFeatureEntry or carbFeatureEntry table
  *
  * @param featureEntryId unique identifier
  * @param featureId      foreign key for [[Feature]] that is referenced
  * @param referenceId    foreign key for table in which this feature was created, sil & carb -> SiliconResults/CarbonResults,
 *                       const -> ProgramEntries
  * @param value          value of the feature
  */
case class FeatureEntry(featureEntryId: Long, featureName: String, referenceId: Long, value: String)

object FeatureEntry {
  def tupled = (FeatureEntry.apply _).tupled
}

/** A wrapper class for an [[AbstractError]] to facilitate comparison and serialization and remove unneeded information
  * Comparison is only done through [[fullId]], since [[message]]s are too specific to a given program
  *
  * @param fullId  the original error ID
  * @param message describes the error in full
  */
case class VerError(fullId: String, message: String) {
  override def equals(obj: Any): Boolean = obj match {
    case that: VerError => this.fullId == that.fullId
    case _              => false
  }

  override def hashCode(): Int = fullId.hashCode
}

/** Result of matching regex to a program
  *
  * @param programEntryId the program in which a match occurred
  * @param matchIndices   the line numbers indicating the start regions of the regex match
  */
case class PatternMatchResult(programEntryId: Long, matchIndices: Seq[Int]) extends Serializable

/** Case class to summarize the difference between [[VerResult]]s different Verifier versions. Only takes into account
  * programs that have a [[VerResult]] for both versions in the database.
  * @param versionHash1: First verifier version
  * @param versionHash2: Second verifier version
  * @param programIntersection List of programEntryIds that have a [[VerResult]] for both verifier versions
  * @param successDiff: List of programEntryIds with different different success values in the [[VerResult]]
  * @param runtimeDiff: List of programEntryIds whose runtime differs by more than 50% in the [[VerResult]]
  * @param errorDiff: List of programEntryIds with different error types in the [[VerResult]]
  * @param avgRuntime1 average runtime of all [[VerResult]]s with [[versionHash1]]
  * @param avgRuntime2 average runtime of all [[VerResult]]s with [[versionHash2]]
  * @param runtimeVar1 Variance of runtime of all [[VerResult]]s with [[versionHash1]]
  * @param runtimeVar2 Variance of runtime of all [[VerResult]]s with [[versionHash2]]
  */
case class VerVersionDifferenceSummary(
  versionHash1: String,
  versionHash2: String,
  programIntersection: Seq[Long],
  successDiff: Seq[Long],
  runtimeDiff: Seq[Long],
  errorDiff: Seq[Long],
  avgRuntime1: Long,
  avgRuntime2: Long,
  runtimeVar1: Long,
  runtimeVar2: Long
)
