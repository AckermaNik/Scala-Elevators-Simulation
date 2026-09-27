# Scala-Akka-Actor-Elevator-Simulation

A concurrent elevator simulation written in Scala with Akka actors. It uses Scala's concise functional style, case-class messages, pattern matching, and collection APIs to model users, elevators, and a coordinator as independent actors that communicate asynchronously.

## Why Scala fits this project

Scala runs on the JVM and combines object-oriented and functional programming. This project uses several of its practical features:

- **Case classes for messages:** `Msg.scala` defines typed event data such as `Tick`, `RequestElevator`, `CallElevator`, and `ElevatorUpdate`. Messages carry the information actors need without sharing mutable state directly.
- **Pattern matching for event handling:** Each actor defines a `receive` function that matches incoming message types and updates its own state. This makes the event-driven control flow explicit.
- **Actor-based concurrency through Akka:** `UserActor`, `ElevatorActor`, and `Coordinator` each own their state and communicate by sending messages. Akka processes each actor's messages sequentially, avoiding direct concurrent access to that actor's mutable fields.
- **Higher-order functions and collections:** Scala collection operations such as `map`, `filter`, `minBy`, and `foreach` create actor sets, filter elevator candidates, select a candidate, and dispatch simulation ticks.
- **JVM build and dependency ecosystem:** SBT compiles the Scala application and resolves Akka and Logback dependencies declared in `build.sbt`.

## Simulation overview

The program simulates users requesting elevator trips over a configured number of floors and minutes. Users wait a randomized interval before requesting service, choose a direction and destination, and accumulate waiting time when an elevator arrives. Elevators track their floor, direction, calls, and passengers. The coordinator keeps the latest status for each elevator and chooses one for each request using its current floor, direction, and idle/busy state.

### Actor roles

- **`UserActor`** models one user. On each `Tick`, it counts down its next request interval. When ready, it chooses a legal travel direction and destination and sends a `RequestElevator` to the coordinator. It reacts to elevator arrival and destination messages to update its floor and waiting state.
- **`ElevatorActor`** models one elevator. It receives calls, queues pickup requests, boards users traveling in its direction, lets passengers exit at their destination, advances between floors, and reports status changes to the coordinator.
- **`Coordinator` / `CoordinatorActor`** track elevator status, select an elevator for each request, and collect completion statistics. Candidate selection considers idle elevators first, then applies direction- and floor-based rules for moving elevators.
- **`Main`** reads configuration from command-line arguments, creates the Akka `ActorSystem` and actors, sends an initialization tick, and then issues minute-by-minute simulation ticks.
- **`Msg`** defines the case-class messages shared by the actors.

### Message-driven workflow

1. `Main` creates the coordinator, elevator actors, and user actors.
2. Initialization ticks cause elevators to publish their initial floor and idle status.
3. For each simulated minute, `Main` sends a `Tick` to users and elevators.
4. A user whose randomized wait has elapsed sends `RequestElevator` to the coordinator.
5. The coordinator selects an elevator and sends it a `CallElevator` message.
6. The elevator queues the call, travels, boards matching users, and sends arrival/update messages as the simulation proceeds.
7. Users update their state when picked up and dropped off. At the configured end time, users and elevators report completion to the coordinator, which logs the aggregate average waiting time and terminates the actor system when all have finished.

The actor model provides concurrency by isolating state behind message boundaries. It does not mean every event runs simultaneously: each individual actor processes its own mailbox sequentially, while different actors can make progress concurrently. `Main` also uses short `Thread.sleep` delays between initialization and minute ticks to pace the simulation.

## Build and run

Requirements: a Java Development Kit, SBT, and network access on the first build so SBT can resolve the declared libraries.

From the project root, run:

```bash
sbt "run <elevators> <floors> <users> <minutes>"
```

For example:

```bash
sbt "run 3 10 20 60"
```

The arguments are the number of elevators, total floors (including the ground floor), users, and simulated minutes. The application reports its expected argument format if fewer than four values are supplied. Logs are configured to print message text to the console through Logback.

## Project structure

- `build.sbt` sets the Scala version and declares Akka Actor and Logback dependencies.
- `project/build.properties` pins the SBT version.
- `src/main/scala/main.scala` starts and paces the simulation.
- `src/main/scala/Coordinator.scala` implements elevator selection, status tracking, and completion reporting.
- `src/main/scala/ElevatorActor.scala` implements elevator movement, pickup queues, and passenger handling.
- `src/main/scala/UserActor.scala` implements user behavior and trip requests.
- `src/main/scala/Msg.scala` defines the actors' typed messages.
- `src/main/resources/logback.xml` configures console logging.
- `target/` and nested SBT target directories are generated build output and can be recreated by SBT.

## Implementation notes

- User requests and initial waiting times use Scala's `Random`, so runs can differ.
- The simulation uses message passing for actor coordination, but its pacing is partly controlled by blocking sleeps in `Main`.
- The coordinator's waiting-time summary divides total waiting time by total calls; scenarios with no calls should be treated as an edge case.
- This is an educational simulation and does not model every real elevator policy or physical timing constraint.
