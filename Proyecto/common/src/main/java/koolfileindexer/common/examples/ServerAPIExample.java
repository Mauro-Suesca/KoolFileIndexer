package koolfileindexer.common.examples;

import koolfileindexer.common.model.File;
import koolfileindexer.common.model.GenericList;
import koolfileindexer.common.model.Search;
import koolfileindexer.common.protocol.Request;
import koolfileindexer.common.protocol.Response;
import koolfileindexer.common.protocol.v1.SocketServer;

public class ServerAPIExample {

    public static void main(String[] args) throws Exception {
        // Inicia un servidor con 10 hilos para trabajos
        SocketServer server = SocketServer.createServer(10);

        // Similar a como HTTP tiene GET, PUT, DELETE aqui uno registra un metodo
        // en este caso "search" y que hacer con este metodo
        // en este caso se simula encontrar dos archivos
        // luego se responde con ok
        server.registerAction("search", (Request req) -> {
            Search s = req.build(Search.stringFactory());
            String[] keywords = s.getKeywords();
            File f = new File(keywords[0], "txt", "/home", "Today", 255, keywords);
            GenericList<File> list = new GenericList<>();
            list.add(f);
            list.add(f);
            return Response.ok(list);
        });

        // Un bucle que acepta cuantas conecciones se necesiten,
        // SocketServer se encarga de gestionar las conecciones con las acciones
        // provistas
        while (true) {
            System.out.println("ready!");
            server.accept();
        }
    }
}
