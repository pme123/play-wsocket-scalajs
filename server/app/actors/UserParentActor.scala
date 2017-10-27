package actors

import javax.inject.Inject

import actors.UserActor.CreateAdapter
import akka.actor._
import akka.event.LoggingReceive
import akka.stream.scaladsl._
import akka.util.Timeout
import play.api.Configuration
import play.api.libs.concurrent.InjectedActorSupport
import play.api.libs.json._
import shared.AdapterMsg

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
/**
 * Provide some DI and configuration sugar for new UserActor instances.
 */
class UserParentActor @Inject()(childFactory: UserActor.Factory,
                                configuration: Configuration)
                               (implicit ec: ExecutionContext)
  extends Actor with InjectedActorSupport with ActorLogging {

  import UserParentActor._
  import akka.pattern.{ask, pipe}

  implicit val timeout = Timeout(2.seconds)

  override def receive: Receive = LoggingReceive {
    case Create(id) =>
      val name = s"userActor-$id"
      log.info(s"Creating initiator actor $name")
      val child: ActorRef = injectedChild(childFactory(id), name)
      val future = (child ? CreateAdapter(id)).mapTo[Flow[AdapterMsg, AdapterMsg, _]]
      pipe(future) to sender()
  }
}

object UserParentActor {
  case class Create(id: String)
}
