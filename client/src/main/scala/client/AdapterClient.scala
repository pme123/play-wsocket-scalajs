package client

import com.thoughtworks.binding.Binding.{Constants, Var, Vars}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.raw._
import org.scalajs.dom.{document, window}
import play.api.libs.json._
import shared._

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.timers.setTimeout

object AdapterClient extends js.JSApp {

  implicit def makeIntellijHappy(x: scala.xml.Elem): Binding[HTMLElement] = ???

  private def runAdapter() {
    println("runAdapter")
    socket.send(Json.toJson(RunAdapter()).toString())
  }

  private val logData = Vars[LogEntry]()
  private val isRunning = Var[Boolean](false)
  private val lastLogLevel = Var[Option[LogLevel]](None)

  private lazy val wsURL = s"ws://${window.location.host}/ws"
  private var socket: WebSocket = _

  private def connectWS() {
    socket = new WebSocket(wsURL)
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

  private def addLogEntries(logReport: LogReport): Unit = {
    logReport.logEntries.foreach(le => logData.value += le)

    val objDiv = document.getElementById("log-panel")
    objDiv.scrollTop = objDiv.scrollHeight - logReport.logEntries.length * 20
  }

  private def addLogEntry(logEntry: LogEntry): Unit = {
    logData.value += logEntry

    val objDiv = document.getElementById("log-panel")
    objDiv.scrollTop = objDiv.scrollHeight - 20
  }

  @dom
  private def logEntry(entry: LogEntry) =
    <div>
      <div class={s"log-level ${entry.level.toString}"}>
        {entry.level.toString}
      </div>
      <div class="log-msg">
        {entry.msg}
      </div>
    </div>

  @dom
  private def render = {
    val runDisabled = isRunning.bind
    val logLevel = lastLogLevel.bind
    <div class="main-panel">
      <div class="button-panel">
        {lastLevel(logLevel.getOrElse("Not run!").toString).bind}<button onclick={event: Event => runAdapter()} disabled={runDisabled}>
        Run Adapter
      </button>
        <button onclick={event: Event => logData.value.clear()}>
          Clear Console
        </button>
      </div>{renderLogEntries.bind}
    </div>
  }

  @dom
  private def lastLevel(lastLevel: String) = {
    <div class={"last-level " + lastLevel}>
      {"Log level last Adapter Process: " + lastLevel}
    </div>
  }
  @dom
  private def renderLogEntries = {
    val logEntries = logData.bind
    <div id="log-panel">
      {Constants(logEntries: _*).map(logEntry(_).bind)}
    </div>
  }

  def main(): Unit = {
    dom.render(document.getElementById("adapter-client"), render)
    connectWS() // initial population
  }
}
