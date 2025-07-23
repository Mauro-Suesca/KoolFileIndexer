package koolfileindexer.common.protocol;

import koolfileindexer.common.Constants;
import koolfileindexer.common.exceptions.InvalidFormatException;
import koolfileindexer.common.model.ErrorMessage;
import koolfileindexer.common.utils.FromStr;
import koolfileindexer.common.utils.IntoStr;
import koolfileindexer.common.utils.Result;

public class Response implements IntoStr {
    public static enum ResultEnum {

        Ok,
        Err,
    }

    private final ResultEnum result;
    private final String data;
    private final FromStr<ErrorMessage> factory = ErrorMessage.stringFactory();

    public Response(ResultEnum result, String data) {
        this.result = result;
        if (data.endsWith(Constants.LINE_SEPARATOR)) {
            data += Constants.LINE_SEPARATOR;
        }
        this.data = data;
    }

    public static Response ok(String data) {
        return new Response(ResultEnum.Ok, data);
    }

    public static Response ok(IntoStr data) {
        return new Response(ResultEnum.Ok, data.intoString());
    }

    public static Response err(String data) {
        return new Response(ResultEnum.Err, data);
    }

    public static Response err(IntoStr data) {
        return new Response(ResultEnum.Err, data.intoString());
    }

    public boolean isError() {
        return this.result == ResultEnum.Err;
    }

    public boolean isOk() {
        return this.result == ResultEnum.Ok;
    }

    public <T> Result<T, ErrorMessage> getData(FromStr<T> factory) throws InvalidFormatException {
        if (isError()) {
            return Result.error(this.factory.from(this.data));
        }
        return Result.success(factory.from(this.data));
    }

    public static FromStr<Response> stringFactory() {
        return source -> {
            try {
                String[] result = source.split(Constants.LINE_SEPARATOR, 2);
                if (result[0].equals("ok")) {
                    return new Response(Response.ResultEnum.Ok, result[1]);
                }
                return new Response(Response.ResultEnum.Err, result[1]);
            } catch (Exception e) {
                throw new InvalidFormatException(Request.class);
            }
        };
    }

    @Override
    public String intoString() {
        String result = "";
        if (isOk()) {
            result += "ok" + Constants.LINE_SEPARATOR;
        } else {
            result += "err" + Constants.LINE_SEPARATOR;
        }
        result += this.data + Constants.LINE_SEPARATOR;
        return result;
    }
}
