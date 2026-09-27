import akka.actor.ActorRef

object Msg{
  case class Tick(minute: Int)
  case class CallElevator(ref: ActorRef,userId: Int, from: Int, to: Int, dir: String,minute: Int)
  case class ElevatorArrived(elevatorId:Int,minute:Int)
  case class ArrivedAtDestination(floor:Int,elevatorId:Int,arrival_min: Int)

  case class UserDone(users_total_waiting: Int, users_total_calls: Int)

  case class ElevatorDone()

  case class RequestElevator(ref: ActorRef,userId: Int, currentFloor: Int, destFloor: Int, direction: String, minute: Int)

  case class ElevatorUpdate(ref: ActorRef, id: Int, currentFloor: Int,direction:String,isIdle: Boolean)

}
