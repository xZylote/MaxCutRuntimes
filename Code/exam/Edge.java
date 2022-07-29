package exam;

class Edge {

	Node node1, node2;
	int weight;

	Edge(Graph graph, int node1, int node2, int weight) {
		this.node1 = graph.getNode(node1);
		this.node2 = graph.getNode(node2);
		this.node1.addToAdjacencyList(this);
		this.node2.addToAdjacencyList(this);
		this.weight = weight;
	}

	@Override
	public String toString() {
		return node1 + "-(" + weight + ")-" + node2;
	}
}
