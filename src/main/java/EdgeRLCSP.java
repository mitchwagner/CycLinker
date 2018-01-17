public class EdgeRLCSP <T> {
    private T tail;
    private T head;
	private double dist;
    private String label;
	
	public EdgeRLCSP(T tail, T head) {
	    this.tail = tail;
	    this.head = head;
	}

	public EdgeRLCSP(T tail, T head, double dist) {
	    this.tail = tail;
	    this.head = head;
	    this.dist = dist;
	}

	public EdgeRLCSP(T tail, T head, String label) {
	    this.tail = tail;
	    this.head = head;
	    this.label = label;
	}

	public EdgeRLCSP(T tail, T head, double dist, String label) {
	    this.tail = tail;
	    this.head = head;
	    this.dist = dist;
	    this.label = label;
	}

	public T getTail() {
	    return this.tail;
	}

	public T getHead() {
	    return this.head;
	}

	public double getDist() {
        return this.dist;
	}

	public String getLabel() {
	    return this.label;
	}

	public boolean labelMatches(EdgeRLCSP other) {
	    return this.label.equals(other.getLabel());
	}
}
