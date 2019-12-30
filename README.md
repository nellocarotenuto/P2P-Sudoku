# P2P-Sudoku
This project is a basic DHT-based implementation of a multiplayer Sudoku game developed for Distributed Systems course
at the University of Salerno.

## Overview
The purpose of this game is to enable Sudoku players to challenge each other over a wide variety of boards. The game
requires a nickname-only based login and allows the creation of multiple challenges that are actually game lobbies where
friends can participate together.

Each player can create a challenge giving it a name and setting its visibility to public or private: public challenges
are listed in the main menu so that everyone who wills to can actually join them while private ones can be accessed only
by the players who know their names.

When participating to a challenge, players do not share the view of the board but can only see the fixed numbers and
those they placed themselves. Each player has a score and earns a point every time he finds a number that no other player
in the challenge has found before while no point is given if the number placed is correct but already found by another
player. A point is dropped instead if the player attempts to place a wrong number. The challenge ends as soon as one
player has completed his own board and the scoreboard is then frozen.

Every challenge has an owner that is the user who created it. This user is the only one capable of starting the
challenge: to avoid playing alone and collecting points before any other player joins, the challenge status is to
waiting upon creation. While in this state, the board is hidden and it is revealed only when the owner sends the
start command which requires at least two players to participate to take effect.

A player can only participate to a challenge at a time and players who quit a challenge are able to join it again but
their score is deleted upon quitting.

## Architecture
The game relies on Peer-to-Peer connectivity and in particular stores its data in a Distributed Hash Table. Every
challenge is put in a location corresponding to the hash of its name while two locations are treated in a special way:
the system is designed to maintain the list of currently logged in players at the location with key zero while the list
of public games is stored in the location with key one of the DHT.

This choice was taken due to technical limitations and validated with workload analysis: the lack of queries that allow
to have a global view of the DHT makes it impractical to notify all the players in the system whenever a new public
challenge is created, for example, as well as locally building the list one challenge at a time. Also, it's worth to
note that the operations on the list of currently logged in players are at most two (addition of an entry upon login and
deletion upon logout) for each player and those on the public challenges list are significantly lower than those
performed on each public challenge.

The list of currently logged in users is transient as the clients do not need to store it nor to keep it updated: every
action is performed by fetching the latest version, updating it locally and pushing it back into the DHT.

A different approach is used for the list of public challenges available in the system: each client keeps a local copy
of the list. Whenever this list is updated, the client performing the operation on it fetches the list, performs the
necessary operation, pushes the result back to the DHT and stores it locally as well. At this point it also sends a
notification to the clients of every user logged into the system which will update their local view of the list by
fetching it from the DHT in response.

Each challenge in the DHT stores the common board and every single view for the users currently playing it. Every time
an action is performed, the user's game client fetches the latest version of the challenge from the DHT, performs the
requested operation and updates the challenge both locally and in the DHT. After that, it sends a notification to the
clients of the users currently participating to the challenge in order to tell them that a new version is available in
the DHT and that they should fetch it to update their local version of the challenge.

DHTs' weak consistency is overcome and concurrent operations all take effect thanks to the vDHT approach. Every update
of an element of the DHT consists of trying to get its latest version until all the peers of the system agree on it.
The modification of the element is performed locally and the resulting version of the element is then put back to the
DHT with a flag indicating the update attempt: if the status of this push is OK for all the replicas, the flag is
removed otherwise the put is cancelled and another attempt is made. In practice this approach works because every put
actually creates a new version of the same element and this makes possible both to check if all peers agree on the same
version and revert put attempts when needed.

## Tools used
This project relies on a number of Open Source Java libraries.

In particular, the core of the application is built with [TomP2P](https://github.com/tomp2p/TomP2P), a library used
for creating Peer-to-Peer connections and to manage the Distributed Hash Table.

[Lanterna](https://github.com/mabe02/lanterna) is used to build the text-based terminal user interface so that the
output is kept clean and always up to date.

To avoid creating too many unnecessary classes, [JavaTuples](https://www.javatuples.org/) has been adopted to provide
type-safe representation of tuples in Java.

Passing parameters from the command line to the application was made simple thanks to
[Args4j](https://github.com/kohsuke/args4j).

[SLF4J](http://www.slf4j.org/) and [Logback](http://logback.qos.ch/) are used for logging during development and test
phases.

Tests for the public interface are made through [JUnit 5](https://junit.org/junit5/) while the whole project is managed
with [Maven](https://maven.apache.org/).

The application is also ready to use with Docker as a Dockerfile is provided for building and executing it.

## Project structure
The project is divided into four different packages that implement different subset of features of the app.

[Sudoku](/src/main/java/com/github/nellocarotenuto/p2psudoku/sudoku) package provides an object-oriented representation
of a Sudoku game. The facade to this package is represented by
[Sudoku](/src/main/java/com/github/nellocarotenuto/p2psudoku/sudoku/Sudoku.java) class.

[Challenge](/src/main/java/com/github/nellocarotenuto/p2psudoku/challenge) package models the entities of the game and
in particular it defines [Player](/src/main/java/com/github/nellocarotenuto/p2psudoku/challenge/Player.java)s,
[Challenge](/src/main/java/com/github/nellocarotenuto/p2psudoku/challenge/Challenge.java)s as well as the
[GameClient](/src/main/java/com/github/nellocarotenuto/p2psudoku/challenge/GameClientImpl.java), the class responsible
for implementing the public API of the game.

[GUI](/src/main/java/com/github/nellocarotenuto/p2psudoku/gui) package contains the only class responsible for serving
game's features through a terminal emulator:
[Console](/src/main/java/com/github/nellocarotenuto/p2psudoku/gui/Console.java).

[Utils](/src/main/java/com/github/nellocarotenuto/p2psudoku/utils) package exposes cleaner DHT APIs and abstracts common
operations that must be made on the DHT in order to guarantee that concurrency doesn't create problems in the system.

## Building
Just like any other project managed with Maven, this application can be built by running ```mvn package``` and this
process can be made faster with the ```-DskipTests=true``` option to avoid performing tests. The version of Java
required here is 8.

A [Dockerfile](/Dockerfile) is also provided to build a container that performs packaging and actually servers the
application via console. To build such container ```docker build --no-cache -t p2p-sudoku .``` command must be used in
the folder where this repository has been cloned.

## Running
The application is packaged in a fat JAR that can be executed though ```java -jar target/p2p-sudoku-1.0.jar``` command
from the project root directory.

There are 3 arguments that can be passed from the command line: ```-ma``` to define the
address of the master peer, ```-mp``` to define the port on which it listens for connections and ```-lp``` to define the
local port to use. By default the first parameter is set to ```localhost```, while the second and third ones are
```4001```. This means that the master peer can be started without passing any parameter while other peers need to pass
at least one parameter between ```-mp``` (if running on a different host) and ```-lp``` (if running on the same host).

For Docker, the application can be run with ```docker run -i -t --name PEER-X p2p-sudoku``` paying attention to specify
the name of the container as well as to append application's arguments at the end, after container's name.

## Usage
<p align="center">
    <img alt="Login screen" height="150" src="/docs/login.png">
    <img alt="Public games screen" height="150" src="/docs/list.png">
    <img alt="Challenge screen" height="150" src="/docs/challenge.png">
</p>

At first, the app asks the user to pick a username and then it shows the list of currently available public challenges
in the system. The user can join a challenge by typing ```!join <challenge>``` or create a new one with
```!public <challenge>``` or ```!private <challenge>``` based on the visibility desired for the challenge to be created.

When participating to a challenge, its owner can type ```!start``` to reveal the board and enable placing numbers. Rows
and columns of the board are labelled with letters from ```a``` to ```i``` and number K can be placed at cell (X, Y) by
typing ```!place KXY```.

Users can quit their current screen and go back to the previous one by typing ```!quit``` from any location.
