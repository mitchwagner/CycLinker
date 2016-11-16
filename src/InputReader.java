import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class InputReader {
	Scanner graphIn;
	Scanner startEnd;

	// These hashmaps map strings for proteins like "P04355" to integers, and
	// vice versa
	// Overflow warning: These integers should never be multiplied, without
	// converting to longs.
	HashMap<String, Integer> mapToInt;
	HashMap<Integer, String> reverseMap;
	
	// List of the starting (source) nodes and ending (target) nodes
	ArrayList<String> starts;
	ArrayList<String> ends;

	// Map of edge number to the edge start point and end point, as well as
	// cost. (order of file read)
	ArrayList<Integer> edgeEndSet;
	ArrayList<Integer> edgeStartSet;
	ArrayList<Double> costs;
	ArrayList<Edge>[] edges;
	ArrayList<Edge>[] reverseEdges;

	// Negative log transform map of edge costs.
	// Key for edge is a perfect hash of the start and end point. (use given
	// function. Works for up to 999,999,999 nodes)
	HashMap<Long, Double> edgeCost;

	public InputReader(String graphFile)
			throws IOException {
		graphIn = new Scanner(new File(graphFile));

		// These hashmaps map strings for proteins like "P04355" to integers,
		// and vice versa
		// Overflow warning: These integers should never be multiplied, without
		// converting to longs.
		mapToInt = new HashMap<String, Integer>();
		reverseMap = new HashMap<Integer, String>();
		
		// Map of edge number to the edge start point and end point, as well as
		// cost.
		edgeEndSet = new ArrayList<Integer>();
		edgeStartSet = new ArrayList<Integer>();
		costs = new ArrayList<Double>();

		// Negative log transform map of edge costs.
		// Key for edge is a perfect hash of the start and end point. (use given
		// function. Works for up to 999,999,999 nodes)
		edgeCost = new HashMap<Long, Double>();
		read();
	}

	@SuppressWarnings("unchecked")
	private void read() {
		int index = 2;
		mapToInt.put("receptor", 0);
		reverseMap.put(0, "receptor");
		mapToInt.put("tf", 1);
		reverseMap.put(1, "tf");
		while (graphIn.hasNext()) {
			String start = graphIn.next();
			// skip lines that are commented out like the header line
			if (start.startsWith("#")){
	            graphIn.nextLine();
				continue;
			}
			String end = graphIn.next();
			double val = graphIn.nextDouble();
			graphIn.nextLine();
			if (!mapToInt.containsKey(start)) {
				mapToInt.put(start, index);
				reverseMap.put(index, start);
				index++;
			}
			if (!mapToInt.containsKey(end)) {
				mapToInt.put(end, index);
				reverseMap.put(index, end);
				index++;
			}
			edgeStartSet.add(mapToInt.get(start));
			edgeEndSet.add(mapToInt.get(end));
			costs.add(val);
		}

		int n = mapToInt.size();
		int e = costs.size();
		edges = new ArrayList[n];
		reverseEdges = new ArrayList[n];
		for (int a = 0; a < n; a++) {
			edges[a] = new ArrayList<Edge>();
			reverseEdges[a] = new ArrayList<Edge>();
		}
		for (int a = 0; a < e; a++) {
			int start = edgeStartSet.get(a);
			int end = edgeEndSet.get(a);
			double distance = (Math.log(costs.get(a)) * -1.0);
			edges[start].add(new Edge(end, distance));
			edgeCost.put(hash(start, end), distance);
			reverseEdges[end].add(new Edge(start, distance));
		}

	}

    public void AddStartEnd(String startEndFile)
			throws IOException {
		// Use these scanners to read files
		startEnd = new Scanner(new File(startEndFile));
		starts = new ArrayList<String>();
		ends = new ArrayList<String>();
		while (startEnd.hasNext()) {
			String startStr = startEnd.next();
			// skip lines that are commented out like the header line
			if (startStr.startsWith("#")){
	            startEnd.nextLine();
				continue;
			}
			String endStr = startEnd.next();
            // end of line. skip to next line
            startEnd.nextLine();
			if (mapToInt.containsKey(startStr) && mapToInt.containsKey(endStr)) {
				int start = mapToInt.get(startStr);
				int end = mapToInt.get(endStr);

				// add an edge from "receptor" to the source (start)
				if (end == 0) {
					edges[end].add(new Edge(start, .00000000000000001));
					reverseEdges[start].add(new Edge(end, .00000000000000001));

					edgeCost.put(hash(start, end), .00000000000000001);
				} 
				// or from the target (end) to "tf"
				else {
					edges[start].add(new Edge(end, .00000000000000001));
					reverseEdges[end].add(new Edge(start, .00000000000000001));

					edgeCost.put(hash(start, end), .00000000000000001);
				}
			}
		}
    }
    
    // remove the "receptor" and "tf" edges from the edges and reverseEdges lists.
    public void RemoveStartEnd(){
    	edges[0] = new ArrayList<Edge>();
    	reverseEdges[1] = new ArrayList<Edge>();
    }
    
    // function to parse the input file and return it as a list of each line.
    public ArrayList<String> ParseFileList(String argFile)
			throws IOException {
    	Scanner s = new Scanner( new File(argFile));
    	ArrayList<String> list = new ArrayList<String>();
    	// read each line and add it to the list
    	while (s.hasNext()){
    		list.add(s.next());
    	}
    	s.close();
    	return list;
    }

	public static long hash(long startNode, long endNode) {
		return startNode * 1000000000l + endNode;
	}

	

}
