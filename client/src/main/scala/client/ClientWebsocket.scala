package client

import client.UIStore._
import org.scalajs.dom.raw._
import org.scalajs.dom.{document, window}
import play.api.libs.json.{JsError, JsSuccess, Json}
import shared._

import scala.scalajs.js.timers.setTimeout

case class ClientWebsocket(uiState: UIState)
  extends UIStore {

  private lazy val wsURL = s"ws://${window.location.host}/ws"

  lazy val socket = new WebSocket(wsURL)

  def connectWS() {

    socket.onmessage = {
      (e: MessageEvent) =>
        val message = Json.parse(e.data.toString)
        message.validate[AdapterMsg] match {
          case JsSuccess(AdapterRunning(logReport), _) =>
            changeAdapterRunning(true)
            addLogEntries(logReport)
          case JsSuccess(AdapterNotRunning(logReport), _) =>
            changeAdapterRunning(false)
            logReport.foreach { lr =>
              changeLastLogLevel(lr)
              addLogEntries(lr)
            }
          case JsSuccess(LogEntryMsg(le), _) =>
            addLogEntry(le)
          case JsSuccess(RunStarted, _) =>
            changeAdapterRunning(true)
          case JsSuccess(RunFinished(logReport), _) =>
            changeAdapterRunning(false)
            changeLastLogLevel(logReport)
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
      append(CLEAR_LOG_ENTRIES)
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

  private def changeAdapterRunning(running: Boolean) {
    append(StoreAction(CHANGE_ADAPTER_RUNNING, Some(running)))
  }

  private def changeLastLogLevel(logReport: LogReport) {
    append(StoreAction(CHANGE_LAST_LOG_LEVEL, Some(logReport.maxLevel())))
  }

  private def addLogEntries(logReport: LogReport) {
    logReport.logEntries.foreach(addLogEntry)
  }

  private def addLogEntry(logEntry: LogEntry) {
    append(StoreAction(ADD_LOG_ENTRY, Some(logEntry)))

    val objDiv = document.getElementById("log-panel")
    objDiv.scrollTop = objDiv.scrollHeight - 20
  }

}
