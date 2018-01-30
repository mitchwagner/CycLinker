import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

public class InputReaderRLCSP {

    // Strings naming the super source and super target nodes
    private static final String SUPER_SOURCE_NAME = "super source";
    private static final String SUPER_TARGET_NAME = "super target";

    // The labels identifying sources and targets in source/target files 
    private static final String SOURCE_NODETYPE_LABEL = "source";
    private static final String TARGET_NODETYPE_LABEL = "target";

    // Node to node ID hashmap for original network
    HashMap<String, Integer> networkNodeToInt;
    HashMap<Integer, String> networkIntToNode;

    // Node to node ID hashmap for product graph
	HashMap<String, Integer> productNodeToInt;
	HashMap<Integer, String> productIntToNode;

    // Edge info data structures for product graph
	// Map of edge number to the edge start point and end point, as well as
	// cost. (order of file read)

    // Map of edge to edge's start/end/cost
	ArrayList<Integer> productEdgeStartSet;
	ArrayList<Integer> productEdgeEndSet;
	ArrayList<Double> productWeights;

	// Each node has an array list of other nodes it is connected to
	ArrayList<Edge>[] productEdges;
	ArrayList<Edge>[] productReverseEdges;

	// Negative log transform map of edge costs.
	// Key for edge is aperfect hash of the start and end point. (use given
	// function. Works for up to 999,999,999 nodes)
	HashMap<Long, Double> productEdgeCost;

    // Map edges in original network to corresponding edges in
    // the product graph. One to many relationship.
	HashMap<Long, ArrayList<Long>> correspondingEdges;

	// Map edges in the product graph. Many to one relationship, hence no
	// need for ArrayList as above
	HashMap<Long, Long> correspondingEdgesReverse;

    // Suppressed because you can't have generic arrays in Java. There should
    // be ways to get around this to remove this statement
    @SuppressWarnings("unchecked")
	public InputReaderRLCSP(File network, File networkSourcesTargets, 
	        File dfa, File dfaSourcesTargets) 
	        throws FileNotFoundException {
        
        ///////////////////////////////////////////////////////////////////////
        // Read nodes from the original network
	    HashSet<String> networkNodes = getNodeList(network);

        // Convert HashSet of nodes to an ArrayList
        ArrayList<String> networkNodesList = 
            new ArrayList<String> (networkNodes);

        networkNodeToInt = new HashMap<String, Integer>();
        networkIntToNode = new HashMap<Integer, String>();

        hashNodes(networkNodesList, networkNodeToInt, networkIntToNode);

        ///////////////////////////////////////////////////////////////////////
        // Read nodes from DFA
	    HashSet<String> dfaNodes = getNodeList(dfa);

        // Get product of the two node sets
	    ArrayList<String> productNodes = 
	        getNodeProduct(networkNodes, dfaNodes);

        // Plus 2 for the super source and super target
        Integer numNodes = productNodes.size() + 2;
        System.out.println("Number of nodes in the product graph: " + 
            numNodes.toString()); 

        // Hash the product nodes. Actually creates two copies of product 
        // nodes, one for each half of the graph.
    
        productNodeToInt = new HashMap<String, Integer>();
        productIntToNode = new HashMap<Integer, String>();

	    hashNodes(productNodes, productNodeToInt, productIntToNode);

        // Read network and DFA edges
        ArrayList<EdgeRLCSP<String>> networkEdges = 
            getNetworkEdgeList(network);
        ArrayList<EdgeRLCSP<String>> dfaEdges = getDFAEdgeList(dfa);

        
        ///////////////////////////////////////////////////////////////////////
        // Product graph edge construction starts here
        productEdgeStartSet = new ArrayList<Integer>();
        productEdgeEndSet = new ArrayList<Integer>();
        productWeights = new ArrayList<Double>();
        productEdges = new ArrayList[numNodes];
        productReverseEdges = new ArrayList[numNodes];

        // Initialize the edges and reverseEdges lists for each node
        for (int i = 0; i < numNodes; i++) {
            productEdges[i] = new ArrayList<Edge>();
            productReverseEdges[i] = new ArrayList<Edge>();
        }

        //////////////////////////////////////////////////////////////////////
        // Creation of product edges
        // (u1, u2) -> (v1, v2) if (u1 -> u2 & v1 -> v2 & label same)

        correspondingEdges = new HashMap<Long, ArrayList<Long>>();
        correspondingEdgesReverse = new HashMap<Long, Long>();

        for (int i = 0; i < networkEdges.size(); i++) {
            EdgeRLCSP<String> networkEdge = networkEdges.get(i);
            for (int j = 0; j < dfaEdges.size(); j++) {
                EdgeRLCSP<String> dfaEdge = dfaEdges.get(j);

                // If our label is the same, create the edge
                if (networkEdge.labelMatches(dfaEdge)) {
                    // Determine the edge's product head and tail nodes
                    String networkTail = networkEdge.getTail();
                    String dfaTail = dfaEdge.getTail();

                    // Create a new node. Use <> to delimit old and new nodes
                    // to avoid problems like TF2 and TF and nodes 1 and 21
                    // BOTH creating compound nodes like TF21
                    // TODO: Abstract this out into a function...
                    String newTail = networkTail + "<>" + dfaTail;

                    String networkHead = networkEdge.getHead();
                    String dfaHead = dfaEdge.getHead();
                    String newHead = networkHead + "<>" + dfaHead;

                    // At this point, the new head and tail nodes should be
                    // product nodes in our hashes. Get their Integer IDs.

                    Integer newTailInt = productNodeToInt.get(newTail);
                    Integer newHeadInt = productNodeToInt.get(newHead);

                    EdgeRLCSP<Integer> productEdge = 
                        new EdgeRLCSP<Integer>(newTailInt, newHeadInt,
                            networkEdge.getDist(), "x");

                    productEdgeStartSet.add(newTailInt);
                    productEdgeEndSet.add(newHeadInt);
                    productWeights.add(networkEdge.getDist());

                    // Track network edge's corresponding edges...
                    Integer networkTailInt = networkNodeToInt.get(networkTail);
                    Integer networkHeadInt = networkNodeToInt.get(networkHead);

                    // First, get the hashed edge IDs
                    Long networkEdgeId = 
                        hash(networkTailInt, networkHeadInt);

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
                    correspondingEdgesReverse.put(
                        productEdgeId, networkEdgeId);
                }
            }
        }

        Integer numEdges = productWeights.size(); 

        productEdgeCost = new HashMap<Long, Double>();

        for (int i = 0; i < numEdges; i++) {
            Integer start = productEdgeStartSet.get(i);
            Integer end = productEdgeEndSet.get(i);

            // Set the cost of the edge as the -log of the edge weight
            // log is the natural log by default
            double cost = (Math.log(productWeights.get(i)) * -1.0);
            productEdges[start].add(new Edge(end, cost));
            productReverseEdges[end].add(new Edge(start, cost));

            productEdgeCost.put(hash(start, end), cost);
        }

        ///////////////////////////////////////////////////////////////////////
        // Add product sources and targets to the graph
        // make the edges from the super source to each product node

        ArrayList<String> networkSources = getSources(networkSourcesTargets);
        ArrayList<String> dfaSources = getSources(dfaSourcesTargets);

        ArrayList<String> productSources = 
            getNodeProduct(networkSources, dfaSources);

        for (String node: productSources) {
            // Get the integer ID of the node. The integer ID of the 
            // supersource is 0.
            Integer id = productNodeToInt.get(node);

            // If the source is actually in the edgelist...
            if (id != null) {
                // Add edge FROM supersource TO source
                productEdges[0].add(new Edge(id, .00000000000000001));

                // Add edge FROM source TO supersource in the reverse graph
                productReverseEdges[id].add(new Edge(0, .00000000000000001));

                // Add edge cost
                productEdgeCost.put(hash(0, id), .00000000000000001);
            }
        }

        // Make edges from the product nodes to the super source node

        ArrayList<String> networkTargets = getTargets(networkSourcesTargets);
        ArrayList<String> dfaTargets = getTargets(dfaSourcesTargets);

        ArrayList<String> productTargets = 
            getNodeProduct(networkTargets, dfaTargets);
        
        for (String node: productTargets) {
            // Get the integer ID of the node. The integer ID of the 
            // supertarget is 1.
            Integer id = productNodeToInt.get(node);

            if (id != null) {
           
                productEdges[id].add(new Edge(1, .00000000000000001));

                productReverseEdges[1].add(new Edge(id, .00000000000000001));

                productEdgeCost.put(hash(id, 1), .00000000000000001);
            }
        }
	}

    // TODO: The code below could be refactored to be generic and iterate over
    // a given file just once.

    /**
     * Read list of nodes specifying a generic network's source set from it's
     * corresponding source-target file.
     */
	public ArrayList<String> getSources(File sourceTargetFile) 
	        throws FileNotFoundException {
	    ArrayList<String> sources = new ArrayList<String>();

	    Scanner scanner = new Scanner(sourceTargetFile);

	    while (scanner.hasNext()) {
	        String node = scanner.next();
	       
	        // Skip comment lines
	        if (node.startsWith("#")) {
	            scanner.nextLine();
	            continue;
	        }
	        else {
	            String nodeType = scanner.next();
	            if (nodeType.equals(SOURCE_NODETYPE_LABEL) || 
	                nodeType.equals("receptor")) {
                    sources.add(node); 
	            }
	            else {
                    // We don't care. Ignore.
	            }
	        }
	        scanner.nextLine();
	    }

        return sources;
	}

    /**
     * Read list of nodes specifying a generic network's target set from it's
     * corresponding source-target file.
     */
	public ArrayList<String> getTargets(File sourceTargetFile) 
	        throws FileNotFoundException {
	    ArrayList<String> targets = new ArrayList<String>();

	    Scanner scanner = new Scanner(sourceTargetFile);

	    while (scanner.hasNext()) {
	        String node = scanner.next();
	       
	        // Skip comment lines
	        if (node.startsWith("#")) {
	            scanner.nextLine();
	            continue;
	        }
	        else {
	            String nodeType = scanner.next();
	            if (nodeType.equals(TARGET_NODETYPE_LABEL) ||
	                nodeType.equals("tf")) {
                    targets.add(node); 
	            }
	            else {
                    // We don't care. Ignore.
	            }
	        }
	        scanner.nextLine();
	    }

	    return targets;
	}

    /** 
     * We'll ultimately be creating two copies of the graph.
     * This method reflects that by doubling the nodes and hashing them.
     */
	private void hashNodes(
	        ArrayList<String> nodes, 
	        HashMap<String, Integer> forwardHash, 
	        HashMap<Integer, String> reverseHash) {

        // Super source
	    forwardHash.put(SUPER_SOURCE_NAME, 0);
	    reverseHash.put(0, SUPER_SOURCE_NAME);

        // Super target
	    forwardHash.put(SUPER_TARGET_NAME, 1);
	    reverseHash.put(1, SUPER_TARGET_NAME);

	    int index = 2;

	    for (int i = 0; i < nodes.size(); i++) {
	        forwardHash.put(nodes.get(i), index);
	        reverseHash.put(index, nodes.get(i));
	        index++;
	    }
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
            Iterable<String> networkNodes, Iterable<String> dfaNodes) {
        
        ArrayList<String> productNodes = new ArrayList<String>();

        for (String networkNode : networkNodes) {
            for (String dfaNode : dfaNodes) {
                String product = networkNode + "<>" + dfaNode;
                productNodes.add(product);
            }
        }

        return productNodes;
    }


    /**
     * Perfect hash for up to 999,999,999 million edges.
     */
	public static long hash(long startNode, long endNode) {
		return startNode * 1000000000l + endNode;
	}
}
