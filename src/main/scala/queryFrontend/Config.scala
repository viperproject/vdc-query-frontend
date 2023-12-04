package queryFrontend

object Config {
  /** Address and port of the API webserver*/
  val API_HOST: String = "http://localhost:8080"
  /** Multiplier used to identify programs that might have different runtimes, i.e. runtimes of t1 and t2 are different
   * if t1 &lt= t2 / MULTIPLIER || t1 >= t2 * MULTIPLIER */
  val TIME_DIFFERENCE_MULTIPLIER: Double = 1.5
}
