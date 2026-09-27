import akka.actor._
import org.slf4j.LoggerFactory
import scala.util.Random
import Msg._

import scala.math.abs

class UserActor(userId: Int, numFloors: Int, elevators: List[ActorRef],available_minutes: Int, coordinator: ActorRef) extends Actor {
  var current_floor = 0
  var wait_time = -1
  var want_elevator = true
  var call_time = 0
  var waiting = Random.between(1, 61)
  var my_destination_floor = -1
  var assigned_elevator: Option[ActorRef] = None
  var total_waiting_time =0;
  var total_calls=0;
  val log = LoggerFactory.getLogger(this.getClass)


  def receive: Receive = {

    case Tick(minute) =>

      if (minute >= available_minutes) {
        coordinator ! UserDone(total_waiting_time,total_calls)
      }else {

        if (want_elevator) {

          waiting -= 1 //1 minute has passed
          if (waiting == 0) {

            want_elevator = false

            // can call elevator
            val direction = current_floor match {
              case 0 => "up"
              case fl if fl == numFloors - 1 => "down"
              case _ => if (Random.nextBoolean()) "up" else "down" // 1==up 0==down
            }

            my_destination_floor = direction match {
              case "up" => Random.between(current_floor + 1, numFloors)
              case "down" => Random.between(0, current_floor)
            }

            // call elevator
            coordinator ! RequestElevator(self,userId, current_floor, my_destination_floor, direction, minute)
            call_time = minute
            total_calls+=1
          }
        }
      }

    case ElevatorArrived(elevatorId,minute) =>
      // enter elevator
      want_elevator = false
      total_waiting_time+=(minute-call_time)


    case ArrivedAtDestination(floor,elevatorId,arrival_min) =>
       // elevator is here
        current_floor = floor
        waiting = Random.between(1, 61) // his next waiting time
        want_elevator = true
  }
}

