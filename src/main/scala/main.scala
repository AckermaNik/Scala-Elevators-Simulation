//> using dep "com.typesafe.akka::akka-actor:2.8.8"

import akka.actor._
import Msg._


import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt}

object Main {
  def main(args: Array[String]): Unit = {

    if(args.length < 4){
      println("Usage: sbt \"run <elevators> <floors> <users> <minutes>\" ")
      return
    }

    val system = ActorSystem("ElevatorSimulation")

    val elevators = args(0).toInt
    val floors = args(1).toInt //including mezzanine/isogeio
    val users = args(2).toInt
    val minutes = args(3).toInt

    val coordinator = system.actorOf(CoordinatorActor.props(users, elevators,system,floors), "coordinator")
    val elevators_list = (0 to elevators-1).map(i => system.actorOf(Props(new ElevatorActor(i, floors,minutes,coordinator)), s"elevator$i")).toList

    elevators_list.foreach(_ ! Tick(-1)) // prepare elevators and let the coordinator know

    val users_list = (0 to users-1).map(i => system.actorOf(Props(new UserActor(i, floors, elevators_list,minutes,coordinator)), s"user$i")).toList

    Thread.sleep(4000) // a small delay to ensure the elevators have been initialized in the coordinator

    // recursive ticks per minute (supposedly)
    def sendTicks(min: Int): Unit = {
      if (min <= minutes) {
        users_list.foreach(_ ! Tick(min))
        elevators_list.foreach(_ ! Tick(min))
        Thread.sleep(800)  //a small delay for a more realistic simulation
        sendTicks(min + 1)
      }
    }

    sendTicks(0)

     // prevent the main thread from exiting too early
    Await.result(system.whenTerminated, Duration.Inf)

  }
}
