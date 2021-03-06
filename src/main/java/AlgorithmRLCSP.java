import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

public class AlgorithmRLCSP {
    // These hashmaps map strings for proteins like "P04355" to integers, and
    // vice versa
    // Overflow warning: These integers should never be multiplied, without
    // converting to longs.
    HashMap<String, Integer> mapToInt;
    HashMap<Integer, String> reverseMap;

    // Map of edge number to the edge start point and end point, as well as
    // cost. (order of file read)
    ArrayList<Integer> edgeEndSet;
    ArrayList<Integer> edgeStartSet;
    ArrayList<Double> weights;
    ArrayList<Edge>[] edges;
    ArrayList<Edge>[] reverseEdges;

    // Negative log transform map of edge costs.
    // Key for edge is a perfect hash of the start and end point. (use given
    // function. Works for up to 999,999,999 nodes)
    HashMap<Long, Double> edgeCost;

    // Used internally in dijkstra's, and in CriticalPath for path printing.
    // Stores the dijkstra's path in symbolic tree form.
    int[] pathForward;
    int[] pathBackward;
    final long INF = Long.MAX_VALUE;


    // For projecting back to the original graph

    HashMap<String, Integer> networkNodeToInt;
    HashMap<Integer, String> networkIntToNode;
    
    HashMap<Long, ArrayList<Long>> correspondingEdges;
    HashMap<Long, Long> correspondingEdgesReverse;

    // Stores the output to pass to the outputWriter class.
    StringBuilder pathOutput;
    StringBuilder edgeOutput;
    StringBuilder correspondingEdgeOutput;

    //maximum number of paths to write
    long maxk;
    long threshold;

    public AlgorithmRLCSP(InputReaderRLCSP graph, long maxK, long threshold) {
        this.threshold = threshold;

        //set up our variables
        mapToInt = graph.productNodeToInt;
        reverseMap = graph.productIntToNode;

        edgeEndSet = graph.productEdgeEndSet;
        edgeStartSet = graph.productEdgeStartSet;
        weights = graph.productWeights;
        edges = graph.productEdges;
        reverseEdges = graph.productReverseEdges;

        edgeCost = graph.productEdgeCost;
        pathOutput = new StringBuilder();
        edgeOutput = new StringBuilder();
        correspondingEdgeOutput = new StringBuilder();

        this.correspondingEdges =
            graph.correspondingEdges;

        this.correspondingEdgesReverse = 
            graph.correspondingEdgesReverse;

        networkNodeToInt = graph.networkNodeToInt;
        networkIntToNode = graph.networkIntToNode;

        maxk = maxK;
    }

    public ArrayList<EdgeRLCSP<String>> readEdgesToCompute(File f) 
            throws FileNotFoundException {
        ArrayList<EdgeRLCSP<String>> list = new ArrayList<EdgeRLCSP<String>>();
        Scanner scanner = new Scanner(f);

        while (scanner.hasNext()) {
            String tail = scanner.next();
            String head = scanner.next();
            EdgeRLCSP<String> edge = new EdgeRLCSP<String>(tail, head);
            list.add(edge);
            scanner.nextLine();
        }

        return list;
    }

    public void run(String edgesToComputeFile) throws FileNotFoundException {

        System.out.println(edgesToComputeFile);

        int start = 0; // super source = 0
        int end = 1; // super sink = 1
        int n = mapToInt.size(); // number of nodes
        int e = weights.size(); // number of edges

        // Dijkstra's from the start, and the end.
        // Stores the cost at from the start to all points.
        final double[] startFromAllNodes = dijkstraFront(n, start, edges);

        // Stores the cost from the end to all points.
        final double[] endFromAllNodes = dijkstra(n, end, reverseEdges);

        // Each edge has a shortest path; We will denote this as a
        // 'CriticalPath'.
        // A critical path does not contain only redundant edges. However, a
        // CriticalPath might be the same as another CriticalPath.
        TreeSet<CriticalPath> potentialPaths = new TreeSet<CriticalPath>();

        
        if (edgesToComputeFile == "") {
            for (int a = 0; a < e; a++) {

                // CriticalPath has 4 parts; Source -> edgeStart -> edgeEnd ->
                // Sink
                CriticalPath tempPath = new CriticalPath(edgeStartSet.get(a),
                        edgeEndSet.get(a), startFromAllNodes, endFromAllNodes,
                        edges);

                potentialPaths.add(tempPath);
            }
        }
        else {
            ArrayList<EdgeRLCSP<String>> edgesToCareAbout = 
                readEdgesToCompute(new File(edgesToComputeFile));

            // Currently no choice but to loop over all product edges
            for (int a = 0; a < e; a++) {
                Integer tail = edgeStartSet.get(a);
                Integer head = edgeEndSet.get(a);

                String productTailString = reverseMap.get(tail);
                String productHeadString = reverseMap.get(head);

                // Loop over all the edges that we actually care about
                for (EdgeRLCSP<String> edge : edgesToCareAbout) {
                    String tailString = edge.getTail();
                    String headString = edge.getHead();
                    
                    // If we care about it, add it
                    if (productTailString.contains(tailString) &&
                        productHeadString.contains(headString)) {

                        CriticalPath tempPath = new CriticalPath(
                            edgeStartSet.get(a),
                            edgeEndSet.get(a), 
                            startFromAllNodes, 
                            endFromAllNodes,
                            edges);

                        potentialPaths.add(tempPath);
                        
                        break;
                    }
                    else {
                        // Do nothing
                    }
                }
            }
        }






        ArrayList<CriticalPath> outputPaths = new ArrayList<CriticalPath>();

        // This was in pathlinker output so I do it as well
        edgeOutput.append("# Tail\tHead\tKSP Index\tPath Cost\n"); 

        int count2 = 0;
        int countPath = 0;

        // This hashmap stores a blacklist of edges that have appeared on
        // earlier paths
        HashSet<Long> ReWriteThisWithEdgeClassLater = new HashSet<Long>();

        HashSet<Long> correspondingEdgeBlacklist = new HashSet<Long>();

        double lastcost = 0;
        long rank = 0;
        // Go through all of the potential paths.
        while (!potentialPaths.isEmpty()) {
            count2++;
            CriticalPath get = potentialPaths.first();
            potentialPaths.remove(get);

            // Start node and end node here define an edge, not 
            // for the path.
            long startNode = get.startNode;
            long endNode = get.endNode;
            outputPaths.add(get);

            if (get.totalCost < this.threshold) {

                // Output the edge
                if (startNode != 0 && endNode != 1 && countPath < maxk) {
                    String outputEdge = reverseMap.get((int) startNode) + "\t"
                            + reverseMap.get((int) endNode) + "\t" + count2 +
                            "\t" + Math.pow(Math.E, -1 * get.totalCost);
                    edgeOutput.append(outputEdge + "\n");
                }

                ///////////////////////////////////////////////////////////////////
                // Projecting the product graph back to G. Remember, our goal here
                // is to find the RLCSP path for an edge in G, not H.
                if (startNode != 0 && endNode != 1 && countPath < maxk) {
                 
                    if (lastcost == get.totalCost) {
                        // Don't do anything
                    }
                    else {
                        rank++;
                        lastcost = get.totalCost;
                    }
                    
                    // TODO: This is a hack that could be made more efficient
                    ArrayList<Integer> pathTemp = get.getPath();
                    for (int b = 0; b < pathTemp.size() - 1; b++) {
                        long hash = hash(pathTemp.get(b), pathTemp.get(b + 1));

                        // Get the parent edge from the original graph's ID
                        Long correspondingEdge = 
                            correspondingEdgesReverse.get(hash);

                        // Not every edge in the critical path will have
                        // a corresponding edge because of super source/sink
                        // edges
                        if (correspondingEdge != null) {
                            // TODO: Should future-proof this or something,
                            // throw it in a method. If the hash function
                            // changes, this must change.
                            long startID = correspondingEdge / 1000000000l;
                            long endID = correspondingEdge % 1000000000l;

                            String startName = 
                                networkIntToNode.get((int)startID);

                            String endName = 
                                networkIntToNode.get((int)endID);

                            if (!correspondingEdgeBlacklist.contains(
                                correspondingEdge)) {

                                correspondingEdgeOutput.append(
                                    startName + "\t"
                                    + endName + "\t"
                                    + rank + "\t"
                                    + Math.pow(Math.E, -1 * get.totalCost)
                                    + "\n");

                                correspondingEdgeBlacklist.add(
                                    correspondingEdge);
                            }
                        }
                    }
                }


                // Get the shortest path that uses that edge.
                ArrayList<Integer> pathTemp = get.getPath();

                // Figure out if this 'criticaledge' is new
                // It can have been seen in an earlier critical path of the same
                // length. We are figuring out if this critical edge is new in
                // order to determine if we need to write a new path out.
                boolean newEdge = false;
                for (int b = 0; b < pathTemp.size() - 1; b++) {
                    long hash = hash(pathTemp.get(b), pathTemp.get(b + 1));
                    if (!ReWriteThisWithEdgeClassLater.contains(hash)) {
                        newEdge = true;
                        ReWriteThisWithEdgeClassLater.add(hash);
                    }
                }

                if (newEdge && countPath < maxk) {
                    countPath++;
                    pathOutput.append(countPath + "\t"
                            + Math.pow(Math.E, -1 * get.totalCost) + "\t"
                            + getString(pathTemp, reverseMap) + "\n");
                }
            }
        }
    }

    // I don't really want to change the output for the ranked paths, but I 
    // want to change or supplement the output for the ranked edges. 

    // Rather than output a1, b2, etc., I just want to output a, b, etc.
    // When I pull an edge ((u, x), (v, y)) from the edgeset, it is guaranteed
    // to be the shortest path containing any product edge corresponding to 
    // (u, v).

    // So, I can keep track of a blacklist of corresponding nodes as well.
    // If the edge has had another (u, v) corresponding edge appear,

    // Okay, let's define some terminology:
    // (u,v) : parent edge
    // ((u, x),(v, y)) : corresponding edge
    // ((u, x),(v, y)) and ((u, a), (v, b)) are sibling edges.

    // For each corresponding edge retrieved from the TreeSet above, look
    // up its parent edge. If its parent edge has already appeared in the 
    // parent edge blacklist, don't print the edge out. 

    // This means inputReader will need one more hashmap: reverse corresponding 

    // Is that useful, though?
    // Yeah, I guess. It's going to be slightly incongruent with the output
    // of the shortest paths themselves, because they'll still keep writing,
    // but that output will just correspond to the shortest paths for H, not
    // for G.

    // Converts a list of node ID's to a list of node names.
    // Ex: [123, 4123] -> "P03422|Q02312"
    private static String getString(ArrayList<Integer> path,
            HashMap<Integer, String> reverseMap) {
        StringBuilder node_names = new StringBuilder();
        for (Integer a : path) {
            if (a != 0 && a != 1) {
                node_names.append(reverseMap.get(a) + "|");
            }
        }
        // remove the last '|'
        String output = node_names.toString();
        output = output.substring(0, output.length()-1);
        return output;
    }

    // Returns a unique 64-bit integer representation of an edge. (hash with no
    // collisions)
    // No two edges can share the same representation.
    private static long hash(long startNode, long endNode) {
        // As long as endNode is below 1 billion and above or equal to 0, this
        // function will never have a collision
        return startNode * 1000000000l + endNode;
    }

	class CriticalPath implements Comparable<CriticalPath> {
		// Start node of the edge defining the 'CriticalPath'
		int startNode;
		// End node of the edge defining the 'CriticalPath'
		int endNode;
        // Cost of the shortest path that uses the edge defined by startNode
        // and endNode.
		double totalCost;

		public CriticalPath(int startNode, int endNode,
				double[] startFromAllNodes, double[] endFromAllNodes,
				ArrayList<Edge>[] edges) {
			this.startNode = startNode;
			this.endNode = endNode;

			this.totalCost = startFromAllNodes[startNode]
					+ endFromAllNodes[endNode]
					+ edgeCost.get(hash(startNode, endNode));
		}

		// If the cost is the same, break ties arbitrarily (we use our hash)
		// compares by cost.
		@Override
		public int compareTo(CriticalPath arg0) {
			if (totalCost == arg0.totalCost) {

				return Long.compare(hash(startNode, endNode),
						hash(arg0.startNode, arg0.endNode));
			}
			return Double.compare(totalCost, arg0.totalCost);
		}

		// Computes the entire path, by traversing the dijkstra's algorithm
		// trees.
		public ArrayList<Integer> getPath() {
			ArrayList<Integer> output = new ArrayList<Integer>();
			ArrayList<Integer> firstHalf = new ArrayList<Integer>();
			int start = startNode;
			while (start != -1) {
				firstHalf.add(start);
				start = pathForward[start];
				if (start == -3) {
					return new ArrayList<Integer>();
				}
			}

			for (int a = firstHalf.size() - 1; a >= 0; a--) {
				output.add(firstHalf.get(a));
			}
			int end = endNode;
			while (end != -1) {
				output.add(end);
				end = pathBackward[end];
				if (end == -3) {
					return new ArrayList<Integer>();
				}
			}
			return output;
		}

		public String toString() {
			return startNode + " " + endNode + " " + totalCost + " "
					+ " Path: " + getString(this.getPath(), reverseMap);
		}
	}

    // /////////////////////////////////////////////////////////////////////////
    // Anything below here is a slightly modified dijkstras SSAD //
    // ////////////////////////////////////////////////////////////////////////
    static class VertexDist implements Comparable<VertexDist> {
        private static final double EPS = 1E-14;
        int vertex;
        double distance;

        public VertexDist(int vertex, double distance) {
            this.vertex = vertex;
            this.distance = distance;
        }

        public String toString() {
            return "(" + vertex + " " + distance + ")";
        }

        @Override
        public int compareTo(VertexDist arg0) {
            VertexDist temp = arg0;
            if (Math.abs(distance - temp.distance) < EPS) {
                return vertex - temp.vertex;
            }
            if (distance < temp.distance) {
                return -1;
            } else {
                return 1;
            }
        }

    }

	/*
	 * Extensively modified.
	 * 
	 * @author Godmar Back
	 */
	double[] dijkstra(int N, int start, ArrayList<Edge>[] edges) {
		final double[] dist = new double[N];
		Arrays.fill(dist, INF);
		dist[start] = 0;
		pathBackward = new int[N];
		Arrays.fill(pathBackward, -3);
		pathBackward[start] = -1;
		TreeSet<VertexDist> frontier = new TreeSet<VertexDist>();

		frontier.add(new VertexDist(start, 0));

		boolean[] optimal = new boolean[N];
		while (!frontier.isEmpty()) {
			VertexDist u2 = frontier.pollFirst();
			int u = u2.vertex;
			if (optimal[u])
				continue;

			optimal[u] = true;
			for (Edge e : edges[u]) {
				double uv = e.dist;
				int v = e.end;
				if (uv != INF) {
					if (dist[u] + uv < dist[v]) {
						dist[v] = dist[u] + uv;
						pathBackward[v] = u;
						frontier.add(new VertexDist(v, dist[v]));
					}
				}
			}
		}
		return dist;
		// 'dist' contains the shortest distance from start to all nodes
	}

    /*
     * Extensively modified.
     * 
     * @author Godmar Back
     */
    double[] dijkstraFront(int N, int start, ArrayList<Edge>[] edges) {
        final double[] dist = new double[N];
        pathForward = new int[N];
        Arrays.fill(pathForward, -3);

        Arrays.fill(dist, INF);
        dist[start] = 0;
        pathForward[start] = -1;
        TreeSet<VertexDist> frontier = new TreeSet<VertexDist>();

        frontier.add(new VertexDist(start, 0));
        boolean[] optimal = new boolean[N];
        while (!frontier.isEmpty()) {
            VertexDist u2 = frontier.pollFirst();
            int u = u2.vertex;
            if (optimal[u])
                continue;

            optimal[u] = true;
            for (Edge e : edges[u]) {
                double uv = e.dist;
                int v = e.end;
                if (uv != INF) {
                    if (dist[u] + uv < dist[v]) {
                        dist[v] = dist[u] + uv;
                        pathForward[v] = u;
                        frontier.add(new VertexDist(v, dist[v]));
                    }
                }
            }
        }
        // 'dist' contains the shortest distance from start to all nodes
        return dist;
    }
}
