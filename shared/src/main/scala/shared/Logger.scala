package shared

import java.time.Instant

import julienrf.json.derived
import play.api.libs.json.OFormat
import slogging.LazyLogging

import scala.util.{Failure, Success, Try}

/**
  * Created by pascal.mengelt on 18.02.2015.
  *
  * Encapsulates Logging - so if changed here is the only place to adjust.
  *
  * This uses the following format info("This is a %1$s text for: %2$s", "sample", "Master")
  * Supported Levels are:
  * - DEBUG
  * - INFO
  * - WARN
  * - ERROR
  */
trait Logger {

  import LogLevel._

  def log(logEntry: LogEntry): LogEntry = logEntry.log()

  def debug(msg: String): LogEntry = LogEntry(DEBUG, msg).log()

  def info(msg: String): LogEntry = LogEntry(INFO, msg).log()

  def warn(msg: String): LogEntry = LogEntry(WARN, msg).log()

  def error(msg: String): LogEntry = LogEntry(ERROR, msg).log()

  def error(exc: Throwable, msg: String): LogEntry = LogEntry(ERROR, msg, exceptionToString(exc)).log()

  def error(exc: Throwable): LogEntry = LogEntry(ERROR, exc.getMessage, exceptionToString(exc)).log()

  private def exceptionToString(exc: Throwable) = Some(exc.getStackTrace.map(_.toString).mkString("\n"))


  def startLog(msg: String): Instant = {
    val dateTime = Instant.now
    LogEntry(INFO, s"$dateTime start: msg").log()
    dateTime
  }

  def endLog(msg: String, startDate: Instant): LogEntry = {
    LogEntry(INFO, s"Finished after ${Instant.now().toEpochMilli - startDate.toEpochMilli} ms: $msg").log()
  }
}

/**
  * Created by pascal.mengelt on 05.03.2015.
  *
  * This is an log entry that can be gathered for a log report.
  * It provides methods for printing the LogEntry.
  */
case class LogEntry(level: LogLevel, msg: String, errorStack: Option[String] = None) {

  lazy val msgWithErrorStack: String = s"$msg ${errorStack.map("\n" + _).getOrElse("")}"

  lazy val asString: String = {
    s"${level.level.toUpperCase}: $msgWithErrorStack"
  }

  def log(): LogEntry = {
    if (level.checkEnabled()) level.log(msgWithErrorStack)
    this
  }

  def asHtmlString: String = s"${level.asHtmlString}: $msgWithErrorStack"

}

object LogEntry {
  implicit val jsonFormat: OFormat[LogEntry] = derived.oformat[LogEntry]()

}

sealed trait LogLevel extends LazyLogging {
  def level: String

  def colorClass: String

  def >=(level: LogLevel): Boolean

  def log(msg: String): Unit

  def checkEnabled(): Boolean

  def asHtmlString = s"""<span class="$colorClass">${level.toUpperCase}</span>"""

}

object LogLevel {
  val unsupportedLogLevel = "Unsupported LogLevel: "

  def fromLevel(level: String): Try[LogLevel] = level.toLowerCase match {
    case "debug" => Success(DEBUG)
    case "info" => Success(INFO)
    case "warn" => Success(WARN)
    case "error" => Success(ERROR)
    case _ =>
      Failure(new IllegalArgumentException(unsupportedLogLevel + level))
  }

  case object DEBUG extends LogLevel {
    val level = "debug"
    val colorClass = "grey"

    override def >=(level: LogLevel): Boolean = level match {
      case DEBUG => true
      case _ => false
    }

    def log(msg: String) {
      logger.debug(msg)
    }

    override def checkEnabled(): Boolean = logger.underlying.isDebugEnabled
  }

  case object INFO extends LogLevel {
    val level = "info"
    val colorClass = "black"

    override def >=(level: LogLevel): Boolean = level match {
      case DEBUG => true
      case INFO => true
      case _ => false
    }

    def log(msg: String) {
      logger.info(msg)
    }

    override def checkEnabled(): Boolean = logger.underlying.isInfoEnabled
  }

  case object WARN extends LogLevel {

    val level = "warn"
    val colorClass = "yellow"

    override def >=(level: LogLevel): Boolean = level match {
      case ERROR => false
      case _ => true
    }

    def log(msg: String) {
      logger.warn(msg)
    }

    override def checkEnabled(): Boolean = logger.underlying.isWarnEnabled
  }

  case object ERROR extends LogLevel {
    val level = "error"
    val colorClass = "red"

    override def >=(level: LogLevel): Boolean = true

    def log(msg: String) {
      logger.error(msg)
    }

    override def checkEnabled(): Boolean = logger.underlying.isErrorEnabled
  }

  implicit val jsonFormat: OFormat[LogLevel] = derived.oformat[LogLevel]()

}
