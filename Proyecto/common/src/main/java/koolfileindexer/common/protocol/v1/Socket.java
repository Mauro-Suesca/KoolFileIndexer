package koolfileindexer.common.protocol.v1;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Optional;

import jnr.unixsocket.UnixSocket;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import koolfileindexer.common.Constants;
import koolfileindexer.common.exceptions.InvalidFormatException;
import koolfileindexer.common.exceptions.InvalidProtocolException;
import koolfileindexer.common.protocol.Request;
import koolfileindexer.common.protocol.Response;
import koolfileindexer.common.utils.ServerFunction;

public class Socket implements Runnable {

    private UnixSocket innerSocket;
    private final String PROTOCOL_HEADER;
    private final String PROTOCOL_TRAILER;
    private final Optional<ServerFunction<Request, Response>> server;

    private Socket(UnixSocket socket, String protocolHeader, String protocolTrailer) {
        this.innerSocket = socket;
        this.PROTOCOL_HEADER = protocolHeader;
        this.PROTOCOL_TRAILER = protocolTrailer;
        this.server = Optional.empty();
    }

    public Socket(UnixSocket socket, String protocolHeader, String protocolTrailer,
            ServerFunction<Request, Response> server) {
        this.innerSocket = socket;
        this.PROTOCOL_HEADER = protocolHeader;
        this.PROTOCOL_TRAILER = protocolTrailer;
        this.server = Optional.of(server);
    }

    public static Socket connect(String protocolHeader, String protocolTrailer)
            throws IOException, SocketException {
        UnixSocketAddress address = new UnixSocketAddress(new File(Constants.SOCKET_PATH));
        UnixSocketChannel channel = UnixSocketChannel.open(address);
        return new Socket(channel.socket(), protocolHeader, protocolTrailer);
    }

    public void close() throws IOException {
        this.innerSocket.close();
    }

    private void write(String data) throws IOException {
        OutputStream outputStream = this.innerSocket.getOutputStream();
        outputStream.write(this.PROTOCOL_HEADER.getBytes());
        for (String line : data.split(Constants.LINE_SEPARATOR)) {
            if (!line.isEmpty()) {
                outputStream.write((line + Constants.LINE_SEPARATOR).getBytes());
            }
        }
        outputStream.write(this.PROTOCOL_TRAILER.getBytes());
        outputStream.flush();
    }

    private String readLine(InputStream reader) throws IOException, InvalidProtocolException {
        StringBuilder sb = new StringBuilder();
        int previous = -1;
        int current;

        while ((current = reader.read()) != -1) {
            if (previous == '\r' && current == '\n') {
                sb.append((char) current);
                break;
            }
            sb.append((char) current);
            previous = current;
        }

        return sb.toString();
    }

    private String read() throws IOException, InvalidProtocolException {
        InputStream reader = this.innerSocket.getInputStream();
        String lines = "";
        String line = this.readLine(reader);
        if (!this.PROTOCOL_HEADER.equals(line)) {
            throw new InvalidProtocolException();
        }
        line = this.readLine(reader);
        int t = 0;
        while (!this.PROTOCOL_TRAILER.equals(line) && t <= Constants.MAX_NUMBER_OF_LINES_ALLOWED) {
            lines += line;
            line = this.readLine(reader);
            t++;
        }
        return lines;
    }

    public Request getRequest() throws IOException, InvalidProtocolException, InvalidFormatException {
        String lines = this.read();

        return Request.stringFactory().from(lines);
    }

    public Response getResponse() throws IOException, InvalidProtocolException, InvalidFormatException {
        String lines = this.read();

        return Response.stringFactory().from(lines);
    }

    public void sendResponse(Response response) throws IOException {
        this.write(response.intoString());
    }

    public void sendRequest(Request request) throws IOException {
        this.write(request.intoString());
    }

    @Override
    public void run() {
        if (this.server.isEmpty()) {
            throw new RuntimeException("No server found... This is meant to be run by the server");
        }
        try {
            Request req = this.getRequest();
            Response res = this.server.get().apply(req);
            this.sendResponse(res);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            try {
                this.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
