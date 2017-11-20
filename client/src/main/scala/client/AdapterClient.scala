package client

import client.UIStore._
import com.thoughtworks.binding.Binding.Constants
import com.thoughtworks.binding.{Binding, dom}
import org.scalajs.dom.document
import org.scalajs.dom.raw._
import org.scalajs.jquery.jQuery
import shared._

import scala.language.implicitConversions
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

object AdapterClient
  extends js.JSApp
    with UIStore {

  val uiState = UIState()

  private lazy val socket = ClientWebsocket(uiState)

  def main(): Unit = {
    dom.render(document.body, render)
    socket.connectWS() // initial population
    import SemanticUI.jq2semantic
    jQuery(".ui.dropdown").dropdown(js.Dynamic.literal(on = "hover"))
  }

  @dom
  private def render = {
    <div>
      {adapterHeader.bind}{//
      adapterContainer.bind}
    </div>
  }

  @dom
  private def adapterHeader = {
    <div class="ui main fixed borderless menu">
      <div class="ui item">
        <img src={"" + g.jsRoutes.controllers.Assets.versioned("images/favicon.png").url}></img>
      </div>
      <div class="ui header item">Reactive Adapter Log Demo</div>
      <div class="right menu">
        {lastLevel.bind}{//
        textFilter.bind}{//
        levelFilter.bind}{//
        runAdapterButton.bind}{//
        clearButton.bind}
      </div>
    </div>
  }

  @dom
  private def lastLevel = {
    val logLevel = uiState.lastLogLevel.bind

    @dom
    def logImage(levelClass: String): Binding[HTMLElement] =
      <i class={"large middle aligned " + levelClass}></i>

    val levelClass: Option[Binding[HTMLElement]] = logLevel
      .map(SemanticUI.levelClass)
      .map(logImage)

    @dom
    def logConstants(levelClass: Option[Binding[HTMLElement]]) =
      Constants(levelClass.toList: _*)
        .map(_.bind)

    <div class="ui item">
      <div class="item"
           data:data-tooltip={"Log level last Adapter Process: " + logLevel.getOrElse("Not run!")}
           data:data-position="bottom center">
        {logConstants(levelClass).bind}
      </div>
    </div>
  }

  // filterInput references to the id of the input (macro magic)
  // this creates a compile exception in intellij
  @dom
  private def textFilter = {
    <div class="ui item">
      <div class="ui input"
           data:data-tooltip="Filter by text."
           data:data-position="bottom right">
        <input id="filterInput"
               type="text"
               placeholder="Filter..."
               onkeyup={_: Event =>
                 append(StoreAction(CHANGE_FILTER_TEXT
                   , Some(s"${filterInput.value}")))}>
        </input>
      </div>
    </div>
  }

  // filterInput references to the id of the input (macro magic)
  // this creates a compile exception in intellij
  @dom
  private def levelFilter = {
    implicit def stringToBoolean(str: String): Boolean = str == "true"

    <div class="ui item"
         data:data-tooltip="Filter the Logs by its Level"
         data:data-position="bottom right">
      <select id="filterSelect"
              class="ui compact dropdown"
              onchange={_: Event =>
                append(StoreAction(CHANGE_FILTER_LEVEL
                  , LogLevel.fromLevel(s"${filterSelect.value}").toOption))}>
        <option value="ERROR">ERROR</option>
        <option value="WARN">WARN</option>
        <option value="INFO">INFO</option>
        <option value="DEBUG" selected="true">DEBUG</option>
      </select>
    </div>
  }

  @dom
  private def runAdapterButton = {
    val runDisabled = uiState.isRunning.bind

    <div class="ui item">
      <button class="ui basic icon button"
              onclick={_: Event => socket.runAdapter()}
              disabled={runDisabled}
              data:data-tooltip="Run the Adapter"
              data:data-position="bottom right">
        <i class="toggle right icon large"></i>
      </button>
    </div>
  }

  @dom
  private def clearButton = {
    <div class="ui item">
      <button class="ui basic icon button"
              onclick={_: Event => append(CLEAR_LOG_ENTRIES)}
              data:data-tooltip="Clear the console"
              data:data-position="bottom right">
        <i class="remove circle outline icon large"></i>
      </button>
    </div>
  }

  @dom
  private def adapterContainer = {
    val logEntries = uiState.logData.bind
    val text = uiState.filterText.bind
    val level = uiState.filterLevel.bind
    val filteredLE =
      logEntries
        .filter(le => le.level >= level)
        .filter(le => le.msg.toLowerCase.contains(text.toLowerCase))

    <div class="ui main text container">
      <div id="log-panel" class="ui relaxed divided list">
        {Constants(filteredLE: _*).map(logEntry(_).bind)}
      </div>
    </div>
  }

  @dom
  private def logEntry(entry: LogEntry) =
    <div class="item">
      <i class={"large middle aligned " + SemanticUI.levelClass(entry.level)}></i>
      <div class="content">
        <div class="description">
          &nbsp;
        </div>
        <div class="header">
          {entry.msg}
        </div>
      </div>
    </div>

  implicit def makeIntellijHappy(x: scala.xml.Elem): Binding[HTMLElement] = ???

}
