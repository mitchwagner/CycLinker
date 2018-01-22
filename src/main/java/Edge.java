/**
 * Bare-bones edge class, meant to be included in the list of 
 * neighbors for a given node. Hence, only the end point and 
 * distance need to be specified.
 */
public class Edge {
	int end;
	double dist;
	
	public Edge(int end, double dist) {
		this.end = end;
		this.dist = dist;
	}
}
