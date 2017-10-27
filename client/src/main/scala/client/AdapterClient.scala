package client

import com.thoughtworks.binding.Binding.{Constants, Var, Vars}
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.raw._
import org.scalajs.dom.{document, window}
import play.api.libs.json._
import shared._

import scala.language.implicitConversions
import scala.scalajs.js

object AdapterClient extends js.JSApp {

  implicit def makeIntellijHappy(x: scala.xml.Elem): Binding[HTMLElement] = ???

  private def runAdapter() {
    println("runAdapter")
    socket.send(Json.toJson(RunAdapter()).toString())
  }

  private val logData = Vars[LogEntry]()
  private val isRunning = Var[Boolean](false)

  private lazy val echo = s"ws://${window.location.host}/ws"
  private lazy val socket = new WebSocket(echo)

  private def websocket() {
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
            addLogEntries(logReport)

          case JsSuccess(LogEntryMsg(le), _) =>
            println(s"Adapter LogEntry: ${le.level}")
            isRunning.value = true
            addLogEntry(le)

          case JsSuccess(RunFinished, _) =>
            println("RunFinished")
            isRunning.value = false

          case JsSuccess(other, _) => println(s"Other message: $other")
          case JsError(errors) => errors foreach println
        }


    }
    socket.onopen = { (e: Event) =>
      println("websocket open!")
      logData.value.clear()
    }
    socket.onclose = {
      (e: CloseEvent) =>
        println("closed socket" + e.reason)
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
      <div>
        {entry.level.toString}
      </div>
      <div>
        {entry.msg}
      </div>
    </div>

  @dom
  private def render = {
    val runDisabled = isRunning.bind
    println("render logEntries")
    <div>
      <button onclick={event: Event => runAdapter()} disabled={runDisabled}>
        Run Adapter
      </button>
      <button onclick={event: Event => logData.value.clear()}>
        Clear Console
      </button>{renderLogEntries.bind}
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
    dom.render(document.body, render)
    websocket() // initial population
  }
}
