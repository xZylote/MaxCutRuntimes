package exam;

import java.util.ArrayList;

class Node {

	int id;
	boolean marked;
	ArrayList<Edge> adjacent = new ArrayList<>();

	Node(int id) {
		this.id = id;
		marked = false;
	}

	void addToAdjacencyList(Edge edge) {
		adjacent.add(edge);
	}

	void mark() {
		marked = true;
	}

	@Override
	public String toString() {
		return "" + id;
	}
}
