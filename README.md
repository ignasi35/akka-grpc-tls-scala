# akka-grpc-tls-scala

A sample Akka-gRPC application with TLS transport.

```
sbt stage
target/universal/stage/bin/greeter-server
```

```
grpcurl -d '{"name":"alice"}' \
  -servername two.example.com  \
  -cacert src/main/resources/exampleca.crt \ 
  -import-path ./src/main/protobuf \
  -proto helloworld.proto localhost:8443 
  GreeterService.SayHello
```