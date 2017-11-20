package client

import com.thoughtworks.binding.Binding.{Var, Vars}
import org.scalajs.dom.raw._
import org.scalajs.dom.{document, window}
import play.api.libs.json.{JsError, JsSuccess, Json}
import shared._

import scala.scalajs.js.timers.setTimeout

case class ClientWebsocket(logData: Vars[LogEntry]
                           , isRunning: Var[Boolean]
                           , filterText: Var[String]
                           , filterLevel: Var[LogLevel]
                           , lastLogLevel: Var[Option[LogLevel]]) {

  private lazy val wsURL = s"ws://${window.location.host}/ws"

  lazy val socket = new WebSocket(wsURL)

  def connectWS() {

    socket.onmessage = {
      (e: MessageEvent) =>
        val message = Json.parse(e.data.toString)
        message.validate[AdapterMsg] match {
          case JsSuccess(AdapterRunning(logReport), _) =>
            println(s"Adapter running")
            isRunning.value = true
            addLogEntries(logReport)
          case JsSuccess(AdapterNotRunning(logReport), _) =>
            println(s"Adapter NOT running")
            isRunning.value = false
            lastLogLevel.value = logReport.map(_.maxLevel())
            logReport.foreach(addLogEntries)
          case JsSuccess(LogEntryMsg(le), _) =>
            isRunning.value = true
            addLogEntry(le)
          case JsSuccess(RunFinished(logReport), _) =>
            println("Run Finished")
            isRunning.value = false
            lastLogLevel.value = Some(logReport.maxLevel())
          case JsSuccess(other, _) =>
            println(s"Other message: $other")
          case JsError(errors) =>
            errors foreach println
        }
    }
    socket.onerror = { (e: ErrorEvent) =>
      println(s"exception with websocket: ${e.message}!")
      socket.close(0, e.message)
    }
    socket.onopen = { (e: Event) =>
      println("websocket open!")
      logData.value.clear()
    }
    socket.onclose = { (e: CloseEvent) =>
      println("closed socket" + e.reason)
      setTimeout(1000) {
        connectWS() // try to reconnect automatically
      }
    }
  }

  def runAdapter() {
    println("run Adapter")
    socket.send(Json.toJson(RunAdapter()).toString())
  }

  private def addLogEntries(logReport: LogReport): Unit = {
    logReport.logEntries.foreach(addLogEntry)
  }

  private def addLogEntry(logEntry: LogEntry): Unit = {
    logData.value += logEntry
    logData.value
      .filter(le => le.level >= filterLevel.value)
      .filter(le => le.msg.contains(filterText.value))

    val objDiv = document.getElementById("log-panel")
    objDiv.scrollTop = objDiv.scrollHeight - 20
  }

}
