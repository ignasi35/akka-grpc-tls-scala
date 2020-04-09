package com.example.helloworld

import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.http.scaladsl.ConnectionContext
import akka.http.scaladsl.HttpsConnectionContext
import akka.http.scaladsl.{Http, HttpConnectionContext}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.UseHttp2.Always
import akka.stream.ActorMaterializer
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

object GreeterServer {

  def main(args: Array[String]): Unit = {
    // important to enable HTTP/2 in ActorSystem's config
    val conf = ConfigFactory
      .parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())
    val system: ActorSystem = ActorSystem("HelloWorld", conf)
    new GreeterServer(system).run()
  }

}

class GreeterServer(system: ActorSystem) {

  def run(): Future[Http.ServerBinding] = {
    implicit val sys = system
    implicit val mat: Materializer = ActorMaterializer()
    implicit val ec: ExecutionContext = sys.dispatcher

    val service: HttpRequest => Future[HttpResponse] =
      GreeterServiceHandler(new GreeterServiceImpl(mat))

    // -----------------
    val sslContext = buildSslContext()
    // ------------

    val context: HttpsConnectionContext = ConnectionContext.https(
      sslContext,
      None,
      None,
      None,
      None,
      None,
      http2 = Always
    )

    Http().setDefaultServerHttpContext(context)
    val bound = Seq(
      Http().bindAndHandleAsync(
        service,
        interface = "127.0.0.1",
        port = 8443,
        connectionContext = context
      ),
      Http().bindAndHandleAsync(
        service,
        interface = "127.0.0.1",
        port = 8080,
        connectionContext = HttpConnectionContext(http2 = Always)
      )
    )

    bound.foreach { fBinding =>
      fBinding.foreach { binding =>
        println(s"gRPC server bound to: ${binding.localAddress}")
      }
    }

    bound.head
  }

  private def buildSslContext(): SSLContext = {
    val password
      : Array[Char] = "3fKrFtcFP9".toCharArray // do not store passwords in code, read them from somewhere safe!

    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    val keystore: InputStream =
      getClass.getClassLoader.getResourceAsStream("example.com.p12")

    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)

    val keyManagerFactory: KeyManagerFactory =
      KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(
      keyManagerFactory.getKeyManagers,
      tmf.getTrustManagers,
      new SecureRandom
    )
    sslContext
  }
}
