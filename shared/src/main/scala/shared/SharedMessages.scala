package shared

import julienrf.json.derived
import play.api.libs.json.OFormat

sealed trait AdapterMsg

object AdapterMsg {
  implicit val jsonFormat: OFormat[AdapterMsg] = derived.oformat[AdapterMsg]()
}

case class RunAdapter(userName: String = "Anonymous") extends AdapterMsg

object RunAdapter {
  implicit val jsonFormat: OFormat[RunAdapter] = derived.oformat[RunAdapter]()
}

case class AdapterRunning(logReport: LogReport) extends AdapterMsg

case class AdapterNotRunning(logReport: Option[LogReport]) extends AdapterMsg

case class LogEntryMsg(logEntry: LogEntry) extends AdapterMsg

case class RunFinished(logReport: LogReport) extends AdapterMsg

case object KeepAliveMsg extends AdapterMsg
