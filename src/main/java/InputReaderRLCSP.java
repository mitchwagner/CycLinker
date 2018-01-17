import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

public class InputReaderRLCSP {

    // Node to node ID hashmap for original network
    HashMap<String, Integer> networkNodeToInt;
    HashMap<Integer, String> networkIntToNode;

    // Node to node ID hashmap for product graph
	HashMap<String, Integer> productNodeToInt;
	HashMap<Integer, String> productIntToNode;

    // Edge info data structures for product graph
	// Map of edge number to the edge start point and end point, as well as
	// cost. (order of file read)
	
	// Given the way I am reading the graph in, these might not be necessary
	ArrayList<Integer> productEdgeStartSet;
	ArrayList<Integer> productEdgeEndSet;
	ArrayList<Double> productWeights;

	// Each node has an array list of other nodes it is connected to
	ArrayList<EdgeRLCSP<Integer>>[] productEdges;
	ArrayList<EdgeRLCSP<Integer>>[] productReverseEdges;

	// Negative log transform map of edge costs.
	// Key for edge is a perfect hash of the start and end point. (use given
	// function. Works for up to 999,999,999 nodes)
	HashMap<Long, Double> productEdgeCost;

    // Map edges in original network to corresponding edges in
    // the product graph
	HashMap<Long, ArrayList<Long>> correspondingEdges;

	public InputReaderRLCSP(File network, File sourcesTargets, File dfa) 
	        throws FileNotFoundException {

        // Read nodes from the original network
	    HashSet<String> networkNodes = getNodeList(network);

        ArrayList<String> networkNodesList = 
            new ArrayList<String> (networkNodes);

        hashNodes(networkNodesList, networkNodeToInt, networkIntToNode);

        // Read nodes from DFA
	    HashSet<String> dfaNodes = getNodeList(dfa);

        // Get product of the two node sets
	    ArrayList<String> productNodes = 
	        getNodeProduct(networkNodes, dfaNodes);

        Integer numNodes = productNodes.size();

        // Hash the product nodes. Actually creates two copies of product 
        // nodes, one for each half of the graph.
	    hashNodes(productNodes, productNodeToInt, productIntToNode);

        // Read network and DFA edges
        ArrayList<EdgeRLCSP<String>> networkEdges = getNetworkEdgeList(network);
        ArrayList<EdgeRLCSP<String>> dfaEdges = getDFAEdgeList(dfa);

        // Make product graph edges
        // We'll actually be creating two copies of each new edge: one for
        // each layer of the resulting graph.
        productEdgeStartSet = new ArrayList<Integer>();
        productEdgeEndSet = new ArrayList<Integer>();
        productWeights = new ArrayList<Double>();
        productEdges = new ArrayList[numNodes];
        productReverseEdges = new ArrayList[numNodes];

        // Initialize the edges abd reverseEdges lists for each node
        for (int i = 0; i < numNodes; i++) {
            productEdges[i] = new ArrayList<EdgeRLCSP<Integer>>();
            productReverseEdges[i] = new ArrayList<EdgeRLCSP<Integer>>();
        }


        // (u1, u2) -> (v1, v2) if (u1 -> u2 & v1 -> v2 & label same)
        for (int i = 0; i < networkEdges.size(); i++) {
            EdgeRLCSP<String> networkEdge = networkEdges.get(i);
            for (int j = 0; j < dfaEdges.size(); j++) {
                EdgeRLCSP<String> dfaEdge = dfaEdges.get(j);

                // If our label is the same, create the edge, and the copy:
                if (networkEdge.labelMatches(dfaEdge)) {
                    // Determine the new head and tail nodes
                    String networkTail = networkEdge.getTail();
                    String dfaTail = dfaEdge.getTail();
                    String newTail = networkTail + dfaTail;
                    String newTailCopy = 
                        duplicateNode(networkTail) + duplicateNode(dfaTail);

                    String networkHead = networkEdge.getHead();
                    String dfaHead = dfaEdge.getHead();
                    String newHead = networkHead + dfaHead;
                    String newHeadCopy = 
                        duplicateNode(networkHead) + duplicateNode(dfaHead);

                    // At this point, the new head and tail nodes should be
                    // product nodes in our hashes. Get their Integer IDs.

                    Integer newTailInt = productNodeToInt.get(newTail);
                    Integer newHeadInt = productNodeToInt.get(newHead);

                    Integer newTailCopyInt = productNodeToInt.get(newTailCopy);
                    Integer newHeadCopyInt = productNodeToInt.get(newHeadCopy);

                    EdgeRLCSP<Integer> productEdge = 
                        new EdgeRLCSP<Integer>(newTailInt, newHeadInt,
                            networkEdge.getDist(), "x");

                    EdgeRLCSP<Integer> productEdgeCopy = 
                        new EdgeRLCSP<Integer>(newTailCopyInt, newHeadCopyInt,
                            networkEdge.getDist(), "x");

                    // Layer one
                    productEdgeStartSet.add(newTailInt);
                    productEdgeEndSet.add(newHeadInt);
                    productWeights.add(networkEdge.getDist());

                    // Layer two
                    productEdgeStartSet.add(newTailCopyInt);
                    productEdgeEndSet.add(newHeadCopyInt);
                    productWeights.add(networkEdge.getDist());

                    // Track network edge's corresponding edges...
                    Integer networkTailInt = networkNodeToInt.get(networkTail);
                    Integer networkHeadInt = networkNodeToInt.get(networkHead);

                    Long networkEdgeId = 
                        hash(networkTailInt, networkHeadInt);

                    // First, Layer One:
                    Long productEdgeId = 
                        hash(newTailInt, newHeadInt);

                    if (!correspondingEdges.containsKey(networkEdgeId)) {
                        ArrayList<Long> list = new ArrayList<Long>();
                        list.add(productEdgeId);

                        correspondingEdges.put(
                            networkEdgeId, list);
                    }
                    else {
                        ArrayList<Long> list = correspondingEdges.get(
                            networkEdgeId);
                        list.add(productEdgeId);
                    }

                    // Then, Layer Two. We've already made sure the ArrayList
                    // is there.
                    Long productEdgeCopyId = 
                        hash(newTailCopyInt, newHeadCopyInt);

                    ArrayList<Long> list = correspondingEdges.get(
                        networkEdgeId);
                    list.add(productEdgeCopyId);

                }
            }
        }

        Integer numEdges = productWeights.size(); 

        for (int i = 0; i < numEdges; i++) {
            Integer start = productEdgeStartSet.get(i);
            Integer end = productEdgeEndSet.get(i);

            // set the cost of the edge as the -log of the edge weight
            // log is the natural log by default
            double cost = (Math.log(productWeights.get(i)) * -1.0);
            productEdges[start].add(new EdgeRLCSP(start, end, cost));
            productReverseEdges[end].add(new EdgeRLCSP(end, start, cost));

            productEdgeCost.put(hash(start, end), cost);
        }

        // TODO: Need to make sure to add super-source and super-sink edges to
        // the network! This needs to be done after creating the network 
        // itself. Any node that corresponds to both a network and a DFA source 
        // node becomes a source node, and similarly for target nodes

	}

    /** 
     * We'll ultimately be creating two copies of the graph.
     * This method reflects that by doubling the nodes and hashing them.
     */
	public void hashNodes(
	        ArrayList<String> nodes, 
	        HashMap<String, Integer> forwardHash, 
	        HashMap<Integer, String> reverseHash) {

        // First Copy 
	    forwardHash.put("super source", 0);
	    reverseHash.put(0, "super source");

	    forwardHash.put("super target", 1);
	    reverseHash.put(1, "super target");

	    int index = 2;

	    for (int i = 0; i < nodes.size(); i++) {
	        forwardHash.put(nodes.get(i), index);
	        reverseHash.put(index, nodes.get(i));
	        index++;
	    }

        // Second Copy
	    forwardHash.put(duplicateNode("super source"), 0);
	    reverseHash.put(0, duplicateNode("super source"));

	    forwardHash.put(duplicateNode("super target"), 1);
	    reverseHash.put(1, duplicateNode("super target"));

	    for (int i = 0; i < nodes.size(); i++) {
	        forwardHash.put(duplicateNode(nodes.get(i)), index);
	        reverseHash.put(index, duplicateNode(nodes.get(i)));
	        index++;
	    }
	}

    /**
     * Utility function for duplicating a node by adding a unique 
     * String to its name.
     */
	public String duplicateNode(String name) {
	    return name + "-2nd Copy";
	}

    /**
     * Read network file.
     * Network files are edgelists where each edge has a weight and a label.
     */
	public ArrayList<EdgeRLCSP<String>> getNetworkEdgeList(File network) 
	        throws FileNotFoundException {

        Scanner scanner = new Scanner(network);

	    ArrayList<EdgeRLCSP<String>> edges = 
	        new ArrayList<EdgeRLCSP<String>>(); 

        while (scanner.hasNext()) {
            String tailNode = scanner.next();
            
            // Skip lines that are commented out
            if (tailNode.startsWith("#")) {
                scanner.nextLine();
                continue;
            }
            String headNode = scanner.next();
            double weight = scanner.nextDouble();
            String label = scanner.next();

            EdgeRLCSP<String> edge = new EdgeRLCSP<String>(
                tailNode, headNode, weight, label);

            edges.add(edge);

            scanner.nextLine();
        }
	    
	    return edges;
	}

    /**
     * Read DFA file.
     * DFA files are edgelists where each edge has a label.
     */
	public ArrayList<EdgeRLCSP<String>> getDFAEdgeList(File dfa) 
	        throws FileNotFoundException {
	        
	    Scanner scanner = new Scanner(dfa);

	    ArrayList<EdgeRLCSP<String>> edges = 
	        new ArrayList<EdgeRLCSP<String>>(); 

        while (scanner.hasNext()) {
            String tailNode = scanner.next();
            
            // Skip lines that are commented out
            if (tailNode.startsWith("#")) {
                scanner.nextLine();
                continue;
            }
            String headNode = scanner.next();
            String label = scanner.next();

            EdgeRLCSP<String> edge = new EdgeRLCSP<String>(
                tailNode, headNode, label);

            edges.add(edge);
            scanner.nextLine();
        }

	    return edges;
	}

    /**
     * Reads a generic network file (in the form of an edgelist) to get
     * the set of nodes in the network.
     * This algorithm expects lines to be of the form "tail \t head ..."
     */
    public HashSet<String> getNodeList(File networkFile) 
            throws FileNotFoundException {

        HashSet<String> nodes = new HashSet<String>();

        Scanner scanner = new Scanner(networkFile);

        while (scanner.hasNext()) {
            String tailNode = scanner.next();
            
            // Skip lines that are commented out
            if (tailNode.startsWith("#")) {
                scanner.nextLine();
                continue;
            }
            String headNode = scanner.next();
            nodes.add(tailNode);
            nodes.add(headNode);
            scanner.nextLine();
        }
        return nodes;
    }

    /**
     * Given the nodes in the network and the nodes in the DFA, get
     * the product of the node sets.
     */
    public ArrayList<String> getNodeProduct(
            HashSet<String> networkNodes, HashSet<String> dfaNodes) {
        
        ArrayList<String> productNodes = new ArrayList<String>();

        for (String networkNode : networkNodes) {
            for (String dfaNode : dfaNodes) {
                String product = networkNode + dfaNode;
                productNodes.add(product);
            }
        }

        return productNodes;
    }

    /**
     * Perfect hash for edges.
     */
	public static long hash(long startNode, long endNode) {
		return startNode * 1000000000l + endNode;
	}
}

    /*

    public void AddStartEnd(String startEndFile, boolean startEndsPenalty, 
            boolean verbose) throws IOException {
		// Use these scanners to read files
		startEnd = new Scanner(new File(startEndFile));
		starts = new ArrayList<Integer>();
		ends = new ArrayList<Integer>();
        int num_sources = 0;
        int num_targets = 0;
		while (startEnd.hasNext()) {
            // first column (start) contains the node name
            // second column (end) contains either 'receptor' or 'tf
			String nodeStr = startEnd.next();
			// skip lines that are commented out like the header line
			if (nodeStr.startsWith("#")){
	            startEnd.nextLine();
				continue;
			}
			String nodeTypeStr = startEnd.next();
			// by default, the cost of a super-source or super-target edge is 0
			double cost = .00000000000000001;
			if (startEndsPenalty) {
				cost = Double.parseDouble(startEnd.next());
			}
            // end of line. skip to next line
            startEnd.nextLine();
            if (mapToInt.containsKey(nodeTypeStr)) {
                if (mapToInt.get(nodeTypeStr) == 0) {
                    num_sources += 1;
                }
                else {
                    num_targets += 1;
                }
            }
			if (mapToInt.containsKey(nodeStr) && 
			        mapToInt.containsKey(nodeTypeStr)) {

                // start is the node name
				int node = mapToInt.get(nodeStr);
                // end is either 'receptor' or 'tf' 
				int rec_or_tf = mapToInt.get(nodeTypeStr);

                // 'receptor' was given the integer value 0, 'tf' was given a 1
                // 'receptor' acts as the super source, 'tf' as the super target
				if (rec_or_tf == 0) {
                    // add an edge from "receptor" to the source (node)
					edges[rec_or_tf].add(new Edge(node, cost));
                    // add a reverse edge from the source to 'receptor'
					reverseEdges[node].add(new Edge(rec_or_tf, cost));

					edgeCost.put(hash(node, rec_or_tf), cost);
                    starts.add(node);
				} 
				// or from the target (node) to "tf"
				else {
					edges[node].add(new Edge(rec_or_tf, cost));
					reverseEdges[rec_or_tf].add(new Edge(node, cost));

					edgeCost.put(hash(node, rec_or_tf), cost);
                    ends.add(node);
				}
			}
		}
    }
    
	*/
