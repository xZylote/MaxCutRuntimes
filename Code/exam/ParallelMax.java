package exam;

import java.util.ArrayList;
import java.util.concurrent.Callable;

class ParallelMax implements Callable<Node> {

	ArrayList<Node> nodes;

	ParallelMax(ArrayList<Node> nodes) {
		this.nodes = nodes;
	}

	/**
	 * Same as in the simple approximation algorithm, this code determines the node
	 * which would improve the weight of all cut edges the most by evaluating how
	 * much cut cost would be added (or subtracted) if a node was marked. However,
	 * here it is only for a part of the nodes.
	 *
	 * This method is called via 'ExecutorService.invokeAll'.
	 */
	@Override
	public Node call() throws Exception {

		Node heaviestNode = null;
		int max = 0;
		for (Node n : nodes) {
			if (!n.marked) {
				int weight = 0;

				for (Edge e : n.adjacent) {
					// "If the other end of the edge is already marked"
					if (!(((e.node2.id != n.id) && !e.node2.marked) || ((e.node1.id != n.id) && !e.node1.marked))) {
						weight -= e.weight;
					} else {
						weight += e.weight;
					}
				}
				// Determine whether the cut cost of this node is the highest
				if (weight > max) {
					max = weight;
					heaviestNode = n;
				}
			}
		}
		return heaviestNode;
	}

}
