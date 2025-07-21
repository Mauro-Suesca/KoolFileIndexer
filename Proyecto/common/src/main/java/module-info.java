module common {
    requires org.jnrproject.unixsocket;
    requires org.objectweb.asm;
    requires org.jnrproject.ffi;

    exports koolfileindexer.common;
    exports koolfileindexer.common.exceptions;
    exports koolfileindexer.common.model;
    exports koolfileindexer.common.protocol;
    exports koolfileindexer.common.protocol.v1;
    exports koolfileindexer.common.utils;
}
