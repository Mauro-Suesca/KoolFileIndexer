package koolfileindexer.common.model;

import java.util.ArrayList;

import koolfileindexer.common.Constants;
import koolfileindexer.common.exceptions.InvalidFormatException;
import koolfileindexer.common.utils.FromStr;
import koolfileindexer.common.utils.IntoStr;

public class GenericList<T extends IntoStr> extends ArrayList<T> implements IntoStr {

    public static <T extends IntoStr> FromStr<GenericList<T>> stringFactory(FromStr<T> t) {
        return source -> {
            try {
                GenericList<T> list = new GenericList<>();
                String[] lines = source.split(Constants.LINE_SEPARATOR);
                Integer size = Integer.parseInt(lines[0].split(": ")[1]);

                int lineIndex = 1;
                for (int i = 0; i < size; i++) {
                    // Find the start of the next item's string representation
                    // Assume each item's string starts with a known prefix (e.g., for File: "name:
                    // ")
                    // and ends before the next item's prefix or end of lines
                    // We'll use the IntoStr format: each item is a block of lines
                    // We'll try to find the next item by looking for the next known prefix or by
                    // counting lines
                    // For generic, we assume each item is separated and occupies a fixed number of
                    // lines or is parseable by t.from()
                    // We'll collect lines until we've gathered enough for t.from()
                    // For simplicity, we'll collect lines until we've reached the start of the next
                    // item or end
                    // Here, we assume each item is separated and occupies a block of lines, so we
                    // try to find the block
                    // We'll use a heuristic: for the last item, take all remaining lines
                    int start = lineIndex;
                    int end = lines.length;
                    if (i < size - 1) {
                        // Try to find the start of the next item by looking for a known prefix
                        // For now, we assume each item is separated by a fixed number of lines
                        // We'll try to parse incrementally until t.from() succeeds
                        // We'll try increasing the number of lines until parsing succeeds
                        boolean parsed = false;
                        for (int j = start + 1; j <= lines.length; j++) {
                            StringBuilder candidate = new StringBuilder();
                            for (int k = start; k < j; k++) {
                                candidate.append(lines[k]);
                                if (k < j - 1)
                                    candidate.append(Constants.LINE_SEPARATOR);
                            }
                            try {
                                T item = t.from(candidate.toString());
                                list.add(item);
                                lineIndex = j;
                                parsed = true;
                                break;
                            } catch (Exception ex) {
                                // keep trying
                            }
                        }
                        if (!parsed) {
                            throw new InvalidFormatException((new GenericList<T>()).getClass());
                        }
                    } else {
                        // Last item: take all remaining lines
                        StringBuilder candidate = new StringBuilder();
                        for (int k = start; k < end; k++) {
                            candidate.append(lines[k]);
                            if (k < end - 1)
                                candidate.append(Constants.LINE_SEPARATOR);
                        }
                        T item = t.from(candidate.toString());
                        list.add(item);
                        lineIndex = end;
                    }
                }

                return list;
            } catch (Exception e) {
                throw new InvalidFormatException((new GenericList<T>()).getClass());
            }
        };
    }

    @Override
    public String intoString() {
        String result = "";

        result += "items: " + this.size() + Constants.LINE_SEPARATOR;

        for (T t : this) {
            result += t.intoString();
        }

        return result;
    }

}
