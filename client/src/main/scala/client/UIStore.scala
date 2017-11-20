package client

import com.thoughtworks.binding.Binding.{Var, Vars}
import shared.{LogEntry, LogLevel}

import UIStore._

trait UIStore {
  def uiState: UIState

  def dispatch(actionType: ActionType) {
    dispatch(StoreAction(actionType))
  }

  def dispatch(action: StoreAction) {
    println(s"Dispatched: $action")
    handleLogData(action)
    handleFilterText(action)
    handleFilterLevel(action)
    handleAdapterRunning(action)
    handleLastLogLevel(action)
  }

  private def handleLogData(action: StoreAction) {
    action.actionType match {
      case CLEAR_LOG_ENTRIES =>
        uiState.logData.value.clear()
      case ADD_LOG_ENTRY =>
        action.payload.foreach(le => uiState.logData.value += le.asInstanceOf[LogEntry])
      case _ => // nothing to do
    }
  }

  private def handleFilterText(action: StoreAction) {
    action.actionType match {
      case CHANGE_FILTER_TEXT =>
        action.payload.foreach(t =>uiState.filterText.value = t.asInstanceOf[String])
      case _ => // nothing to do
    }
  }

  private def handleFilterLevel(action: StoreAction) {
    action.actionType match {
      case CHANGE_FILTER_LEVEL =>
        action.payload.foreach(le => uiState.filterLevel.value = le.asInstanceOf[LogLevel])
      case _ => // nothing to do
    }
  }

  private def handleAdapterRunning(action: StoreAction) {
    action.actionType match {
      case CHANGE_ADAPTER_RUNNING =>
        action.payload.foreach(running => uiState.isRunning.value = running.asInstanceOf[Boolean])
      case _ => // nothing to do
    }
  }

  private def handleLastLogLevel(action: StoreAction) {
    action.actionType match {
      case CHANGE_LAST_LOG_LEVEL =>
        uiState.lastLogLevel.value =
          action.payload.map(lle => lle.asInstanceOf[LogLevel])
      case _ => // nothing to do
    }
  }
}

object UIStore {

  case class UIState(logData: Vars[LogEntry] = Vars[LogEntry]()
                     , isRunning: Var[Boolean] = Var[Boolean](false)
                     , filterText: Var[String] = Var[String]("")
                     , filterLevel: Var[LogLevel] = Var[LogLevel](LogLevel.DEBUG)
                     , lastLogLevel: Var[Option[LogLevel]] = Var[Option[LogLevel]](None)
                    )

  case class StoreAction(actionType: ActionType
                         , payload: Option[Any] = None)

  trait ActionType

  case object CLEAR_LOG_ENTRIES extends ActionType

  case object ADD_LOG_ENTRY extends ActionType

  case object CHANGE_FILTER_TEXT extends ActionType

  case object CHANGE_FILTER_LEVEL extends ActionType

  case object CHANGE_ADAPTER_RUNNING extends ActionType

  case object CHANGE_LAST_LOG_LEVEL extends ActionType


}
