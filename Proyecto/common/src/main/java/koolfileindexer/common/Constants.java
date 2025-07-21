package koolfileindexer.common;

public class Constants {

    public static final String APP_NAME = "KoolFileIndexer";
    public static final String SOCKET_PATH = "/run/user/1000/koolfileindexer.socket";
    public static final int MAX_NUMBER_OF_LINES_ALLOWED = Integer.MAX_VALUE - 10;
    public static final String LINE_SEPARATOR = "\r\n";
    public static final String PROTOCOL_HEADER = "koolfileindexer v1" + LINE_SEPARATOR;
    public static final String PROTOCOL_TRAILER = "end v1" + LINE_SEPARATOR;
}
