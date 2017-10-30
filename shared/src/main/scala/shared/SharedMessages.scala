package shared

import julienrf.json.derived
import play.api.libs.json.OFormat

/**
  * all needed messages for server-client communication.
  */
sealed trait AdapterMsg

object AdapterMsg {
  // marshalling and unmarshalling
  // with json.validate[AdapterMsg] or Json.parse(adapterMsg)
  // this line is enough with this library - as AdapterMsg is a sealed trait
  implicit val jsonFormat: OFormat[AdapterMsg] = derived.oformat[AdapterMsg]()
}

case class RunAdapter(userName: String = "Anonymous") extends AdapterMsg

case class AdapterRunning(logReport: LogReport) extends AdapterMsg

case class AdapterNotRunning(logReport: Option[LogReport]) extends AdapterMsg

case class LogEntryMsg(logEntry: LogEntry) extends AdapterMsg

case class RunFinished(logReport: LogReport) extends AdapterMsg

case object KeepAliveMsg extends AdapterMsg
