package koolfileindexer.common.model;

import koolfileindexer.common.Constants;
import koolfileindexer.common.exceptions.InvalidFormatException;
import koolfileindexer.common.utils.FromStr;
import koolfileindexer.common.utils.IntoStr;

public class ErrorMessage implements IntoStr {
    private String errorMessage;

    public ErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public static FromStr<ErrorMessage> stringFactory() {
        return source -> {
            try {
                String[] lines = source.split(Constants.LINE_SEPARATOR);

                String errorMessage = lines[0].split(": ")[1];

                return new ErrorMessage(errorMessage);
            } catch (Exception e) {
                throw new InvalidFormatException(Search.class);
            }
        };
    }

    @Override
    public String intoString() {
        String result = "";
        result += "error-message: " + this.errorMessage + Constants.LINE_SEPARATOR;
        return result;
    }
}
