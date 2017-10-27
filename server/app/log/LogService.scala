package log

import java.time.{Instant, LocalDateTime, ZoneId}

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import shared.LogLevel._
import shared.{LogEntry, LogLevel, LogReport}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/**
  * service that wraps the LogReport
  */
case class LogService(name: String, initiator: String)
                     (implicit mat: Materializer, ec: ExecutionContext) {

  val instant: Instant = Instant.now
  val logReport = LogReport(initiator)

  def print() {
    logReport.info(logReport.createPrint(DEBUG))
  }

  def logs(): List[String] = logReport.logEntries
    .map(le => le.asString).toList

  def log(logLevel: LogLevel, msg: String): LogEntry =
    logReport + logReport.log(LogEntry(logLevel, msg))

  def debug(msg: String): LogEntry =
    logReport + logReport.debug(msg)

  def info(msg: String): LogEntry =
    logReport + logReport.info(msg)

  def warn(msg: String): LogEntry =
    logReport + logReport.warn(msg)

  def error(msg: String): LogEntry =
    logReport + logReport.error(msg)

  def error(exc: Throwable, msg: String): LogEntry =
    logReport + logReport.error(exc, msg)


  def startLogging(): LogEntry = {
    info(s"$name started at ${LocalDateTime.ofInstant(instant, ZoneId.systemDefault())}")
  }

  def stopLogging(): LogEntry = {
    info(s"Import $name took ${Instant.now.toEpochMilli - instant.toEpochMilli} ms.")
  }

  def stopLogging(clientId: String): LogEntry = {
    info(s"Import $name for $clientId took ${Instant.now.toEpochMilli - instant.toEpochMilli} ms.")
  }

}
