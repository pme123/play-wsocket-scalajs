package client

import com.thoughtworks.binding.Binding.{Var, Vars}
import shared.{LogEntry, LogLevel, LogReport}

trait UIStore {
  protected def uiState: UIState

  protected def clearLogData() {
    println("UIStore: clearLogData")
    uiState.logData.value.clear()
  }

  protected def addLogEntry(logEntry: LogEntry) {
    println(s"UIStore: addLogEntry $logEntry")
    uiState.logData.value += logEntry
  }

  protected def changeFilterText(text: String) {
    println(s"UIStore: changeFilterText $text")
    uiState.filterText.value = text
  }

  protected def changeFilterLevel(logLevel: LogLevel) {
    println(s"UIStore: changeFilterLevel $logLevel")
    uiState.filterLevel.value = logLevel
  }

  protected def changeIsRunning(running: Boolean) {
    println(s"UIStore: changeIsRunning $running")
    uiState.isRunning.value = running
  }

  protected def changeLastLogLevel(report: LogReport) {
    println(s"UIStore: changeLastLogLevel ${report.maxLevel()}")
    uiState.lastLogLevel.value = Some(report.maxLevel())
  }
}

case class UIState(logData: Vars[LogEntry] = Vars[LogEntry]()
                   , isRunning: Var[Boolean] = Var[Boolean](false)
                   , filterText: Var[String] = Var[String]("")
                   , filterLevel: Var[LogLevel] = Var[LogLevel](LogLevel.DEBUG)
                   , lastLogLevel: Var[Option[LogLevel]] = Var[Option[LogLevel]](None)
                  )
