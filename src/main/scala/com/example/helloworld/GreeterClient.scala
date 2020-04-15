package com.example.helloworld

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import akka.Done
import akka.NotUsed
import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import akka.stream.SystemMaterializer
import akka.stream.scaladsl.Source

object GreeterClient {

  def main(args: Array[String]): Unit = {
    implicit val sys = ActorSystem("HelloWorldClient")
    implicit val mat = SystemMaterializer(sys).materializer
    implicit val ec = sys.dispatcher

    val client = GreeterServiceClient(
      GrpcClientSettings
        .fromConfig("tls.helloworld.GreeterService")
    )

    val names =
      if (args.isEmpty) List("Alice", "Bob")
      else args.toList

    names.foreach(singleRequestReply)

    if (args.nonEmpty)
      names.foreach(streamingBroadcast)

    def singleRequestReply(name: String): Unit = {
      println(s"Performing request: $name")
      val reply = client.sayHello(HelloRequest(name))
      reply.onComplete {
        case Success(msg) =>
          println(msg)
        case Failure(e) =>
          println(s"Error: $e")
      }
    }

    def streamingBroadcast(name: String): Unit = {
      println(s"Performing streaming requests: $name")

      val requestStream: Source[HelloRequest, NotUsed] =
        Source
          .tick(1.second, 1.second, "tick")
          .zipWithIndex
          .map { case (_, i) => i }
          .map(i => HelloRequest(s"$name-$i"))
          .mapMaterializedValue(_ => NotUsed)

      val responseStream: Source[HelloReply, NotUsed] =
        client.sayHelloToAll(requestStream)
      val done: Future[Done] =
        responseStream.runForeach(
          reply => println(s"$name got streaming reply: ${reply.message}")
        )

      done.onComplete {
        case Success(_) =>
          println("streamingBroadcast done")
        case Failure(e) =>
          println(s"Error streamingBroadcast: $e")
      }
    }
  }

}
