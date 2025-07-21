package koolfileindexer.common.examples;

import koolfileindexer.common.protocol.Request;
import koolfileindexer.common.protocol.Response;
import koolfileindexer.common.protocol.v1.SocketServer;

public class ServerAPIExample {

    public static void main(String[] args) throws Exception {
        SocketServer server = SocketServer.createServer(10);

        server.registerAction("search", (Request a) -> {
            return Response.ok(a.getRawData());
        });

        while (true) {
            System.out.println("ready!");
            server.accept();
        }
    }
}
