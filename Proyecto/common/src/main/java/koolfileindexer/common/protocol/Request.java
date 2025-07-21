package koolfileindexer.common.protocol;

import koolfileindexer.common.Constants;
import koolfileindexer.common.exceptions.InvalidFormatException;
import koolfileindexer.common.utils.FromStr;
import koolfileindexer.common.utils.IntoStr;

public class Request implements IntoStr {
    private final String method;
    private final String data;

    public Request(String method, String data) {
        this.method = method;
        this.data = data;
    }

    public Request(String method, IntoStr data) {
        this.method = method;
        this.data = data.intoString();
    }

    public static FromStr<Request> stringFactory() {
        return source -> {
            try {
                String[] result = source.split(Constants.LINE_SEPARATOR, 2);
                return new Request(result[0], result[1]);
            } catch (Exception e) {
                throw new InvalidFormatException(Request.class);
            }
        };
    }

    public String getMethod() {
        return method;
    }

    public String getRawData() {
        return data;
    }

    public <T> T build(FromStr<T> factory) throws InvalidFormatException {
        return factory.from(this.data);
    }

    @Override
    public String intoString() {
        String result = "";
        result += this.method + Constants.LINE_SEPARATOR;
        result += this.data + Constants.LINE_SEPARATOR;
        return result;
    }
}
