package actors

import javax.inject.Inject

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import akka.stream.Materializer
import log.LogService
import shared.LogLevel._
import shared._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

/**
  * This actor contains a set of stocks internally that may be used by
  * all websocket clients.
  */
class AdapterActor @Inject()(implicit mat: Materializer, ec: ExecutionContext)
  extends Actor
    with ActorLogging {

  private var logService: LogService = _

  private var isRunning = false

  val clientActors: mutable.Map[String, ActorRef] = mutable.Map()


  def receive = LoggingReceive {
    case SubscribeAdapter(clientId, wsActor) =>
      log.info(s"called AdapterStatus for: $clientId")
      val aRef = clientActors.getOrElseUpdate(clientId, wsActor)
      val status = if (isRunning) AdapterRunning(logService.logReport)
      else AdapterNotRunning(if (logService != null) Some(logService.logReport) else None)
      aRef ! status
    case UnSubscribeAdapter(clientId) =>
      log.info(s"called Unsubscribe for: $clientId")
      clientActors -= clientId
    case RunAdapter(user) =>
      log.info(s"called runAdapter: $user")

      if (isRunning)
        log.warning("The adapter is running already!")
      else {
        log.info(s"run Adapter: $sender")

        logService = LogService("Demo Adapter Process", user)
        isRunning = true
        Future {
          sendToSubscriber(logService.startLogging())

          for (i <- 0 to 10) {
            Thread.sleep(750)
            val ll = Random.shuffle(List(DEBUG, DEBUG, INFO, INFO, INFO, WARN, WARN, ERROR)).head
            sendToSubscriber(logService.log(ll, s"Adapter Process $ll: $i"))
          }
          sendToSubscriber(logService.stopLogging())
          isRunning = false
          sendToSubscriber(RunFinished(logService.logReport))
        }
      }
    case other =>
      log.info(s"unexpected message: $other")
  }

  private def sendToSubscriber(logEntry: LogEntry): Unit =
    sendToSubscriber(LogEntryMsg(logEntry))

  private def sendToSubscriber(adapterMsg: AdapterMsg): Unit =
    clientActors.values
      .foreach(_ ! adapterMsg)
}

case class SubscribeAdapter(clientId: String, wsActor: ActorRef)

case class UnSubscribeAdapter(clientId: String)
