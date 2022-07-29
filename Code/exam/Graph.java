package exam;

import java.util.ArrayList;

class Graph {

	ArrayList<Node> nodeList;
	ArrayList<Edge> edgeList;
	int nodeCount, edgeCount;

	Graph() {
		nodeList = new ArrayList<>();
		edgeList = new ArrayList<>();
	}

	void addNode(Node x) {
		nodeList.add(x);
	}

	void addEdge(Edge x) {
		edgeList.add(x);
	}

	Node getNode(int id) {
		return nodeList.get(id - 1);
	}

	ArrayList<Node> getNodeList() {
		return nodeList;
	}

	void resetMarks() {
		for (Node n : nodeList) {
			n.marked = false;
		}
	}

	void setNodeEdgeCount(int nodeCount, int edgeCount) {
		this.nodeCount = nodeCount;
		this.edgeCount = edgeCount;
		System.out.println(nodeCount + " Vertices and " + edgeCount + " Edges");
	}

}
