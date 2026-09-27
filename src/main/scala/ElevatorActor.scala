import akka.actor._
import org.slf4j.LoggerFactory
import scala.collection.mutable.{ListBuffer, Queue}
import Msg._

import scala.concurrent.duration.DurationInt

class ElevatorActor(elevatorId: Int, numFloors: Int,available_minutes: Int,coordinator: ActorRef) extends Actor {
  var currentFloor = 0
  var number_of_users=0
  var stop_minute = -2;
  var direction: String = "up"
  var passengers: ListBuffer[(ActorRef, Int,String,Int)] = ListBuffer() // (UserActor, destinationFloor)
  var calls: Queue[(ActorRef,Int,Int, Int, String,Int)] = Queue() // (UserActor, from floor, to, direction)
  var moving = false
  val log = LoggerFactory.getLogger(this.getClass)

  def receive: Receive = {

    case Tick(minute) =>

      if(minute== -1)   coordinator ! ElevatorUpdate(self, elevatorId, 0, "up",true)
      else {
        if (minute >= available_minutes) {
          coordinator ! ElevatorDone()
        } else {
          if (moving && stop_minute != minute) {
            moveElevator(minute) // 1 min has passed so elevator has arrived to a destination
          }
        }
      }


    case CallElevator(user_actor,userId, from , to,userDirection,minute) =>

      calls.enqueue((user_actor,userId ,from, to, userDirection, minute))

      log.info(s"Minute $minute: User $userId calls elevator $elevatorId at floor $from to go to floor $to")

      //in case it was stopped and starting moving again after it's first call
      if(moving==false ) {

        moving = true

        stop_minute=minute //don't move in the same minute

        //heading straight to my first caller
        if(from<currentFloor) {
          direction = "down"
          currentFloor-=1
          coordinator ! ElevatorUpdate(self, elevatorId, currentFloor, direction,false)
          //log.info(s"NEW/or not DIRECTION FOR ELEVATOR $elevatorId : " + direction)
        }
        else if(from>currentFloor) {
          direction="up"
          currentFloor+=1
          coordinator ! ElevatorUpdate(self, elevatorId, currentFloor, direction,false)
          //log.info(s"NEW/or not DIRECTION FOR ELEVATOR $elevatorId : " + direction)

        }
        else{ //same floor
          if(direction!=userDirection) {
            //log.info(s"NEW DIRECTION FOR ELEVATOR $elevatorId : " + userDirection + " with same floor: " + currentFloor)
            direction = userDirection
          }
          coordinator ! ElevatorUpdate(self, elevatorId, currentFloor, direction,false)
        }
      }


    case _ =>
  }

  def moveElevator(minute: Int): Unit = {

    if (currentFloor == 0) direction = "up"
    if (currentFloor == numFloors - 1) direction = "down"

    log.info(s"Minute $minute: Elevator $elevatorId reaches floor $currentFloor")

    //is someone stepping in?

    val boarding = calls.dequeueAll {
      case (_,_, from, _, userDir, _) =>
        from == currentFloor && userDir == direction
    }

    //passengers can now enter
    
    boarding.foreach {
      case (calling_actor,user_id ,from, to, userDirection, _) =>
        calling_actor ! ElevatorArrived(elevatorId,minute)
        passengers += ((calling_actor, to, direction, user_id))
        number_of_users += 1
        log.info(s"Minute ${minute}: User $user_id enters elevator $elevatorId at floor $from")
    }


    // is someone stepping out?

    val exiting = passengers.filter(_._2 == currentFloor)

    //passengers can now exit

    exiting.foreach {
      case (user,_,_,user_id) =>
        user ! ArrivedAtDestination(currentFloor,elevatorId,minute)
        log.info(s"Minute $minute: User $user_id exits elevator $elevatorId at floor $currentFloor ")
        passengers --= passengers.filter {
          case (u, floor, _,_) => u == user && floor == currentFloor
      }
      number_of_users-=1
    }

    //no ones is in the elevator , no one is boarding and no one is calling it
    if ( number_of_users==0 && boarding.isEmpty && calls.isEmpty) {
      log.info(s"Minute $minute: Elevator $elevatorId stopped at floor $currentFloor (no passengers or calls)")
      moving = false
      coordinator ! ElevatorUpdate(self, elevatorId, currentFloor, direction,true)
    }else{
      //next floor
      currentFloor = direction match {
        case "up" => currentFloor + 1
        case "down" => currentFloor - 1
      }
      coordinator ! ElevatorUpdate(self, elevatorId, currentFloor, direction,false)
    }

    // wait a min for passengers to walk out or in
    context.system.scheduler.scheduleOnce(1.seconds, self, "stopCompleted")(context.dispatcher)//a small delay for a more realistic simulation

  }
}
