package actors

import javax.inject.Inject

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import akka.stream.Materializer
import log.LogService
import shared.LogLevel.DEBUG
import shared._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
  * This actor contains a set of stocks internally that may be used by
  * all websocket clients.
  */
class AdapterActor @Inject()(implicit mat: Materializer, ec: ExecutionContext)
  extends Actor
    with ActorLogging {

  private var logService: LogService = LogService("Never running", "by config")

  private var isRunning = false

  val clientActors: mutable.Map[String, ActorRef] = mutable.Map()


  def receive = LoggingReceive {
    case SubscribeAdapter(clientId, wsActor) =>
      log.info(s"called AdapterStatus for: $clientId")
      val aRef = clientActors.getOrElseUpdate(clientId, wsActor)
      val status = if (isRunning) AdapterRunning(logService.logReport) else AdapterNotRunning(logService.logReport)
      log.info("LOG-REPORT: "+ logService.logReport.createPrint(DEBUG))
      aRef ! status
    case RunAdapter(user) =>
      log.info(s"called runAdapter: $user")

      if (isRunning)
        log.warning("The adapter is running already!")
      else {
        log.info(s"run Adapter: $sender")

        logService = LogService("Demo Adapter", user)
        isRunning = true
        Future {
          sendToSubscriber(logService.startLogging())

          for (i <- 0 to 10) {
            Thread.sleep(750)
            sendToSubscriber(logService.warn(s"Adapter Process: $i"))
          }
          sendToSubscriber(logService.stopLogging())
          isRunning = false
          sendToSubscriber(RunFinished)
        }
      }
    case other =>
      log.info(s"unexpected message: $other")
  }

  private def sendToSubscriber(logEntry: LogEntry): Unit =
    sendToSubscriber(LogEntryMsg(logEntry))

  private def sendToSubscriber(adapterMsg: AdapterMsg): Unit =
    clientActors.values.foreach(_ ! adapterMsg)
}

case class SubscribeAdapter(clientId: String, wsActor: ActorRef)
