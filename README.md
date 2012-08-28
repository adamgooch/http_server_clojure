# http-server

This is a simple HTTP server capable of serving web pages.  It runs on port 5000.
A request of / will return "Hello World".  A request of a directory will return
the contents of that directory.  A Host: header is required in the request as
required by the HTTP1.1 protocol.  Persistent connections have not yet been
implemented.

## Usage
Make your current working directory the project folder then enter:

   java -jar target/http-server-0.1.0-SNAPSHOT-standalone.jar

at the console.  The server runs on port 5000 by default.  Press
CNTRL C to stop the server.

If you want to point the server at a specific directory, simply enter
the path of the directory after the launching command.

   java -jar target/http-server-0.1.0-SNAPSHOT-standalone.jar /Users/Public
