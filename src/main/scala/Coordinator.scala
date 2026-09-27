import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import Msg._
import org.slf4j.LoggerFactory


class Coordinator(totalUsers: Int, totalElevators: Int, system: ActorSystem, total_floors: Int) extends Actor {
  var usersDone = 0
  var elevatorsDone = 0
  var waiting_sum=0
  var calls_sum=0
  val log = LoggerFactory.getLogger(this.getClass)
  var elevatorStatuses: List[( ActorRef, Int, Int, String, Boolean)] = List()


  def receive: Receive = {


    case UserDone(users_total_waiting,users_total_calls) =>
      waiting_sum+=users_total_waiting
      calls_sum+=users_total_calls
      usersDone += 1
      checkCompletion()

    case ElevatorDone() =>
      elevatorsDone += 1
      checkCompletion()

    case ElevatorUpdate(ref, id, floor, direction,isIdle) =>
      val newStatus = (ref, id,floor,direction, isIdle)
      elevatorStatuses = elevatorStatuses.filterNot(_._2 == id) :+ newStatus

    case RequestElevator(user_actor,userId, users_floor, destFloor, users_direction, minute) =>

      //choose the optimal elevator

      //start by checking the idle ones
      val idleElevators = elevatorStatuses.filter(_._5==true)

      val chosenElevator =
        if (idleElevators.nonEmpty) {
          idleElevators.minBy(e => math.abs(e._3 - users_floor))._1
        }
        else {

          //if there are no idle ones check if I am in the 2 special floors
          if (users_floor == total_floors - 1 ) { //users direction down - all elevators are below
            val elevators_below_me_that_go_up = elevatorStatuses.filter(e => e._4 == "up")

            if (elevators_below_me_that_go_up.nonEmpty) {
              elevators_below_me_that_go_up.minBy(e => math.abs(e._3 - users_floor))._1
            } else {
              val elevators_below_me_that_go_down = elevatorStatuses.filter(e => e._4 == "down")
              elevators_below_me_that_go_down.maxBy(e => math.abs(e._3 - users_floor))._1
            }

          } else if (users_floor == 0) { //users direction up - all elevators are above
            val elevators_above_me_that_go_down = elevatorStatuses.filter(e => e._4 == "down")

            if (elevators_above_me_that_go_down.nonEmpty) {
              elevators_above_me_that_go_down.minBy(e => math.abs(e._3 - users_floor))._1
            } else {
              val elevators_above_me_that_go_up = elevatorStatuses.filter(e => e._4 == "up")
              elevators_above_me_that_go_up.maxBy(e => math.abs(e._3 - users_floor))._1
            }
          } else {

            //otherwise according to user's direction and elevator's next floor and direction, I find the optimal elevator for the user.
            if (users_direction == "up") {
              val elevators_below_me = elevatorStatuses.filter(e => e._3 < users_floor)
              if (elevators_below_me.nonEmpty) {
                  //1rst optimal case
                val elevators_below_me_that_go_up = elevators_below_me.filter(e => e._4 == "up")
                if (elevators_below_me_that_go_up.nonEmpty)
                  elevators_below_me_that_go_up.minBy(e => math.abs(e._3 - users_floor))._1
                else {
                    //2nd optimal case
                  val elevators_below_me_that_go_down = elevators_below_me.filter(e => e._4 == "down")
                  elevators_below_me_that_go_down.maxBy(e => math.abs(e._3 - users_floor))._1
                }
              } else {
                val elevators_above_me = elevatorStatuses.filter(e => e._3 >= users_floor)
                  //3rd optimal case
                val elevators_above_me_that_go_down = elevators_above_me.filter(e => e._4 == "down")
                if (elevators_above_me_that_go_down.nonEmpty)
                  elevators_above_me_that_go_down.minBy(e => math.abs(e._3 - users_floor))._1
                else {
                    //4rth optimal case
                  val elevators_above_me_that_go_up = elevators_above_me.filter(e => e._4 == "up")
                  elevators_above_me_that_go_up.maxBy(e => math.abs(e._3 - users_floor))._1
                }
              }

            } else {
              val elevators_above_me = elevatorStatuses.filter(e => e._3 > users_floor)
              if (elevators_above_me.nonEmpty) {
                  //1rst optimal case
                val elevators_above_me_that_go_down = elevators_above_me.filter(e => e._4 == "down")
                if (elevators_above_me_that_go_down.nonEmpty)
                  elevators_above_me_that_go_down.minBy(e => math.abs(e._3 - users_floor))._1
                else {
                  //2nd optimal case
                  val elevators_above_me_that_go_up = elevators_above_me.filter(e => e._4 == "up")
                  elevators_above_me_that_go_up.maxBy(e => math.abs(e._3 - users_floor))._1
                }
              } else {
                val elevators_below_me = elevatorStatuses.filter(e => e._3 <= users_floor)
                //3rd optimal case
                val elevators_below_me_that_go_up = elevators_below_me.filter(e => e._4 == "up")
                if (elevators_below_me_that_go_up.nonEmpty)
                  elevators_below_me_that_go_up.minBy(e => math.abs(e._3 - users_floor))._1
                else {
                    //4rth optimal case
                  val elevators_below_me_that_go_down = elevators_below_me.filter(e => e._4 == "down")
                  elevators_below_me_that_go_down.maxBy(e => math.abs(e._3 - users_floor))._1
                }
              }
            }
          }
        }


      chosenElevator ! CallElevator(user_actor,userId, users_floor, destFloor, users_direction, minute)

  }

  def checkCompletion(): Unit = {

    if (usersDone >= totalUsers && elevatorsDone  >= totalElevators ) {
      log.info("Simulation finished")
      log.info(s"Average waiting time of users:  ${waiting_sum/calls_sum} minutes.")
      system.terminate()
    }
  }
}


object CoordinatorActor {
  def props(totalUsers: Int, totalElevators: Int,system: ActorSystem,total_floors: Int): Props =
    Props(new Coordinator(totalUsers, totalElevators,system,total_floors))
}



