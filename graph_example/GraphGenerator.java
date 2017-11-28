import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.Charset;

public class GraphGenerator {
  public static void generate(Set<Edge> edges) {
    HashSet<Edge> existingEdges = new HashSet<>();
    StringBuilder dotFile = new StringBuilder("digraph sitemap {\n");
    for (Edge e: edges) {
      String trimFrom = trimUrl(e.getFrom());
      String trimTo = trimUrl(e.getTo());
      Edge trimEdge = new Edge(trimFrom,trimTo);
      if (existingEdges.contains(trimEdge))
        continue;
      else {
        dotFile.append("\"").append(trimFrom).append("\" -> \"").append(trimTo).append("\"\n");
        existingEdges.add(trimEdge);
      }
    }
    dotFile.append("}");

    List<String> lines = Arrays.asList(dotFile.toString());
    Path file = Paths.get("graph.dot");
    try {
      Files.write(file, lines, Charset.forName("UTF-8"));
      Runtime rt = Runtime.getRuntime();
      rt.exec("dot -Tpng graph.dot -o graph.png");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static String trimUrl(String url) {
    String[] urlSplit = url.split("/");
    if (urlSplit.length < 3) return "";
    String trimmedUrl = urlSplit[2];
    StringBuilder returnUrl = new StringBuilder("");
    for (char c: trimmedUrl.toCharArray()) {
      if (c != '?')
        returnUrl.append(c);
      else
        break;
    }
    return returnUrl.toString();
  }
}
