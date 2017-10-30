# Websockets with Play Framework, Scala.js, Binding.scala

This project is based on:
* [Algomancer's Starter Kit](https://github.com/Algomancer/Full-Stack-Scala-Starter)
* [Lightbend's Websocket example](https://github.com/playframework/play-scala-websocket-example)

See the general setup on the starter kit page.

This is an example application showing how you can integrate a Play project with a Scala.js, Binding.scala project - using Web Sockets.

It's about an automatic process that can be started manually (button). But it should run only once at a time.

So only one actor (AdapterActor) will run the process and make sure, that the process is only run once at a time.

The web-sockets are created according to the example.

Each client sees the LogEntries of the last 'Adapter process' (LogReport) - or if the process is running - each LogEntry right away.

The Binding.scala takes care of:
* show the LogEntries
* disable the 'Run Adapter' button
* show the last LogLevel of the Adapter Process

## Run the application
```shell
$ sbt
> run
$ open http://localhost:9000
```
