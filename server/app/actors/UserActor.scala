package actors

import javax.inject._

import actors.UserActor.CreateAdapter
import akka.actor._
import akka.event.{LogMarker, MarkerLoggingAdapter}
import akka.pattern.ask
import akka.stream._
import akka.stream.scaladsl._
import akka.util.Timeout
import akka.{Done, NotUsed}
import com.google.inject.assistedinject.Assisted
import play.api.libs.json._
import shared._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Creates a initiator actor that sets up the websocket stream.  Although it's not required,
  * having an actor manage the stream helps with lifecycle and monitoring, and also helps
  * with dependency injection through the AkkaGuiceSupport trait.
  *
  * @param adapterActor the actor responsible for stocks and their streams
  * @param ec           implicit CPU bound execution context.
  */
class UserActor @Inject()(@Assisted id: String, @Named("adapterActor") adapterActor: ActorRef)
                         (implicit mat: Materializer, ec: ExecutionContext)
  extends Actor {

  // Useful way to mark out individual actors with websocket request context information...
  private val marker = LogMarker(name = self.path.name)
  implicit val log: MarkerLoggingAdapter = akka.event.Logging.withMarker(context.system, this.getClass)

  implicit val timeout = Timeout(50.millis)

  val (hubSink, hubSource) = MergeHub.source[JsValue](perProducerBufferSize = 16)
    .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
    .run()

  private var adapterInit = false

  private val jsonSink: Sink[JsValue, Future[Done]] = Sink.foreach { json =>
    // When the initiator runs the Adapter
    json.validate[RunAdapter] match {
      case JsSuccess(runAdapter: RunAdapter, _) =>
        adapterActor ! runAdapter
      case JsSuccess(other, _) =>
        log.info(s"Unexpected message from ${sender()}: $other")
      case JsError(errors) =>
        log.error(marker, "Other than RunAdapter: " + errors.toString())
    }
  }

  // If this actor is killed directly, stop anything that we started running explicitly.
  override def postStop(): Unit = {
    log.info(marker, s"Stopping actor $self")
  }

  /**
    * The receive block, useful if other actors want to manipulate the flow.
    */
  override def receive: Receive = {
    case CreateAdapter(clientId) =>
      log.info(s"asked for AdapterStatus")
      adapterActor ! SubscribeAdapter(clientId, wsActor())

      sender() ! websocketFlow

    case other =>
      log.info(s"Unexpected message from ${sender()}: $other")
  }

  /**
    * Generates a flow that can be used by the websocket.
    *
    * @return the flow of JSON
    */
  private lazy val websocketFlow: Flow[JsValue, JsValue, NotUsed] = {
    // Put the source and sink together to make a flow of hub source as output (aggregating all
    // stocks as JSON to the browser) and the actor as the sink (receiving any JSON messages
    // from the browse), using a coupled sink and source.
    Flow.fromSinkAndSourceCoupled(jsonSink, hubSource)
      .watchTermination() { (_, termination) =>
        // When the flow shuts down, make sure this actor also stops.
        termination.foreach(_ => context.stop(self))
        NotUsed
      }
  }

  /**
    * Adds several stocks to the hub, by asking the stocks actor for stocks.
    */
  private def run(runAdapter: RunAdapter) = {
    log.info(s"runAdapter>>>>>>>: $runAdapter")
    adapterActor ! runAdapter
  }

  /**
    * Adds a single stock to the hub.
    */
  private def wsActor(): ActorRef = {
    // We convert everything to JsValue so we get a single stream for the websocket.
    // Make sure the history gets written out before the updates for this stock...
    val historySource = Source.actorRef(Int.MaxValue, OverflowStrategy.fail)


    // Set up a complete runnable graph from the stock source to the hub's sink
    Flow[AdapterMsg]
      .keepAlive(1.minute, () => KeepAliveMsg)
      .map(Json.toJson[AdapterMsg])
      .to(hubSink)
      .runWith(historySource)

  }


}


object UserActor {

  trait Factory {
    def apply(id: String): Actor
  }

  case class CreateAdapter(clientId: String)

}



