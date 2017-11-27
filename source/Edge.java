import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Edge implements Serializable {

    private String from;
    private String to;

    public Edge(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public List<String> toList() {
        return Arrays.asList(from, to);
    }

    @Override
    public String toString() {
        return toList().toString();
    }
}
