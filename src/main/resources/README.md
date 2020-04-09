# Certificates

The certificates and keys in this folder were created using the 
[`play-scala-tls-example` scripts](https://github.com/playframework/play-samples/tree/2.8.x/play-scala-tls-example/scripts). Use the `exampleca.crt` 
on the gRPC client so the client trusts on the server certificate (which is issued with that CA).

The resquests must use `one.example.com`, `two.example.com` or `www.example.com` as the `authority` for the server to locate a valid certificate 
and serve the request.