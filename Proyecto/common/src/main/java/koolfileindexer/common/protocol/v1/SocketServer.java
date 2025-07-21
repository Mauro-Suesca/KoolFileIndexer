package koolfileindexer.common.protocol.v1;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jnr.unixsocket.UnixServerSocket;
import jnr.unixsocket.UnixSocket;
import jnr.unixsocket.UnixSocketAddress;
import koolfileindexer.common.Constants;
import koolfileindexer.common.exceptions.MethodNotFoundException;
import koolfileindexer.common.protocol.Request;
import koolfileindexer.common.protocol.Response;
import koolfileindexer.common.utils.ServerFunction;

public class SocketServer {

    private UnixServerSocket innerSocket;
    private HashMap<String, ServerFunction<Request, Response>> methods;
    private ExecutorService threadPool;
    private static final String PROTOCOL_HEADER = "koolfileindexer v1\r\n";
    private static final String PROTOCOL_TRAILER = "end v1\r\n";

    private SocketServer(UnixServerSocket socket, Integer threadPoolSize) {
        this.innerSocket = socket;
        this.methods = new HashMap<>();
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
    }

    public static SocketServer createServer(Integer threadPoolSize) throws SocketException, IOException {
        File path = new File(Constants.SOCKET_PATH);
        path.deleteOnExit();
        UnixSocketAddress address = new UnixSocketAddress(path);
        UnixServerSocket socket = new UnixServerSocket();
        socket.bind(address);
        return new SocketServer(socket, threadPoolSize);
    }

    public void accept() throws IOException {
        UnixSocket socket = this.innerSocket.accept();
        Socket s = new Socket(socket, PROTOCOL_HEADER, PROTOCOL_TRAILER, (Request req) -> {
            String method = req.getMethod();
            ServerFunction<Request, Response> function = this.methods.get(method);
            if (function == null) {
                throw new MethodNotFoundException(method);
            }
            return function.apply(req);
        });
        this.threadPool.submit(s);
    }

    public void registerAction(String methodName, ServerFunction<Request, Response> method) {
        this.methods.put(methodName, method);
    }
}
