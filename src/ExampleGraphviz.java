import java.util.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.charset.Charset;

public class ExampleGraphviz {
	
	HashMap<String,Set<String>> urlMap = new HashMap<String,Set<String>>();

	public void add(String x,String y) {
	    Set<String> set = urlMap.get(x);
	    if (set == null) {
	        urlMap.put(x, set = new HashSet<String>());
	    } 
	    set.add(y);
	}

	public void drawGraph() {
		//	1. Format output
		//	2. Save output
		//  3. Call on graphviz

		// Create the dot file
		String dotFile = "digraph sitemap {\n";

		for (String key : urlMap.keySet()) {
            
            for (String entry : urlMap.get(key)) {
            	dotFile += ("\"" + key + "\" -> \"" + entry + "\"\n");
            }
            
        }

		dotFile += "}";

		List<String> lines = Arrays.asList(dotFile);
		Path file = Paths.get("file.dot");
		try {
			Files.write(file, lines, Charset.forName("UTF-8"));
			
			// Call the graph
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec("dot -Tpng file.dot -o graph.png");
		} catch (Exception e) {
			System.out.println("Lazy Exception Handling");	
		}
		
		

	}

	public static void main(String args[]){
		
		ExampleGraphviz egv = new ExampleGraphviz();

		egv.add("str1.com", "str1.com/akira");
		egv.add("str1.com", "str1.com/batman");
		egv.add("str1.com", "str1.com/commodore");
		egv.add("str1.com", "str1.com/dig-dug");
		egv.add("str1.com", "str1.com/EVIL");
		egv.add("str1.com/akira", "str1.com/batman");
		egv.add("str1.com/batman", "str1.com/akira");
		egv.add("str1.com/batman", "str1.com/commodore");
		egv.add("str1.com/batman", "str1.com/dig-dug");
		egv.add("str1.com/batman", "str1.com/giant");
		egv.add("str1.com/batman", "str1.com/bomb");

		egv.drawGraph();

	}

}
