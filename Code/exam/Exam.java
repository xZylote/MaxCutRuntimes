package exam;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

/**
 * All code written by me. Idea for ILP formulation from Sera Kahruman, Elif
 * Kolotoglu,Sergiy Butenko, and Illya V. Hicks.
 */
public class Exam {
	/**
	 * Reads file, executes all four Max-Cut algorithms and prints the results.
	 */
	public static void maxCut(String filename) {
		Graph graph = new Graph();
		String[] metadata = read(filename, graph);
		System.out.println("Graph name: " + metadata[0].replace('_', ' '));
		graph.setNodeEdgeCount(Integer.parseInt(metadata[1]), Integer.parseInt(metadata[2]));

		System.out.println("______________________________");
		System.out.println("HEURISTIC:");
		long time1 = System.nanoTime();

		heuristic(graph);
		results(graph, time1);

		System.out.println("______________________________");
		System.out.println("2-APPROXIMATION:");
		graph.resetMarks();
		long time2 = System.nanoTime();

		approximation(graph);
		results(graph, time2);

		System.out.println("______________________________");
		System.out.println("2-APPROXIMATION PARALLEL:");
		graph.resetMarks();
		long time3 = System.nanoTime();

		approximationParallel(graph);
		results(graph, time3);

		System.out.println("______________________________");
		System.out.println("ILP:");
		graph.resetMarks();
		long time4 = System.nanoTime();

		if (Integer.parseInt(metadata[2]) < 20000) {
			ILP(graph);
			results(graph, time4);
		} else {
			System.out.println("Cannot solve graphs with more than 20000 edges");
		}

	}

	/**
	 * Orders the list of all edges by weight and then marks one of the nodes of the
	 * first (and therefore heaviest) edge. Then deletes all edges that share a node
	 * with aforementioned edge (itself included). Repeatedly marks the first edge
	 * in the list and deletes all other edges who share a node with it. Repeat
	 * until no more edges remain. Works arbitrarily bad on complete graphs.
	 */
	public static void heuristic(Graph graph) {

		ArrayList<Edge> edges = new ArrayList<>(graph.edgeList);
		// Sort edges by weight in n log n
		Collections.sort(edges, (o1, o2) -> Integer.compare(o2.weight, o1.weight));

		while (!edges.isEmpty()) {
			Node heaviestEdgeU = edges.get(0).node1;
			Node heaviestEdgeV = edges.get(0).node2;
			// Mark one of the nodes of the heaviest edge
			heaviestEdgeU.mark();

			// Prevent all edges adjacent to the selected one from being selected next
			for (int i = 0; i < edges.size(); i++) {
				if ((edges.get(i).node1.id == heaviestEdgeU.id) || (edges.get(i).node2.id == heaviestEdgeU.id)
						|| (edges.get(i).node1.id == heaviestEdgeV.id) || (edges.get(i).node2.id == heaviestEdgeV.id)) {
					edges.remove(i);
					i--;
				}
			}
		}
	}

	/**
	 * Marks the node which would improve the weight of all cut edges the most by
	 * evaluating how much cut cost would be added (or subtracted) if a node was
	 * marked. After doing this, start over until no improvement can be made.
	 */
	public static void approximation(Graph graph) {
		// We could also use a while(true) loop, since the actual condition is that no
		// improvement is made. However, this will certainly happen in |V| steps anyway.
		for (int i = 0; i < graph.nodeCount; i++) {
			int max = 0;
			Node heaviestNode = null;

			for (Node n : graph.nodeList) {
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
			if (heaviestNode == null) {
				// We are done if heaviestNode is not assigned, meaning there can no longer be
				// made any improvement
				break;
			}
			heaviestNode.mark();
		}
	}

	/**
	 * Variation of the sequential approximation algorithm above. When analyzing
	 * which node would improve the weight of all cut edges the most, subdivides the
	 * list of nodes into as many parts as there are cores and gets the node which
	 * would add most cut cost from every sublist. Then evaluates which one of the
	 * results from every list is the globally best option and marks the
	 * corresponding node. Start over until no improvement can be made.
	 */
	public static void approximationParallel(Graph graph) {
		int nodeCount = graph.nodeCount;
		int cores = Runtime.getRuntime().availableProcessors();

		// We create threads and assign each one the task of finding the node with
		// maximum cut cost improvement if marked in their part of the node list.
		int partSize = (nodeCount / cores) + 1;
		ExecutorService executor = Executors.newFixedThreadPool(cores);
		for (int i = 0; i < nodeCount; i++) {
			List<Callable<Node>> tasks = new ArrayList<>();
			for (int j = 0; j < nodeCount; j += partSize) {
				ParallelMax task = new ParallelMax(
						new ArrayList<>(graph.nodeList.subList(j, Math.min(j + partSize, nodeCount))));
				tasks.add(task);

			}
			try {
				Node heaviestNode = getGlobalMax(executor, tasks);
				// We are done if heaviestNode is not assigned, meaning that no thread has found
				// an improvement in their sublist
				if (heaviestNode == null) {
					break;
				}
				heaviestNode.mark();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		executor.shutdown();
	}

	/**
	 * @return Maximum of results of all provided tasks
	 */
	private static Node getGlobalMax(ExecutorService executor, List<Callable<Node>> tasks)
			throws InterruptedException, ExecutionException {

		List<Future<Node>> results = executor.invokeAll(tasks);
		int max = 0;
		Node heaviestNode = null;

		for (Future<Node> result : results) {
			Node n = result.get();
			if (n != null) { // Some threads might return null while others don't
				int weight = 0;
				for (Edge e : n.adjacent) {
					// "If the other end of the edge is already marked"
					if (!(((e.node2.id != n.id) && !e.node2.marked) || ((e.node1.id != n.id) && !e.node1.marked))) {
						weight -= e.weight;
					} else {
						weight += e.weight;
					}
				}

				if (weight > max) {
					max = weight;
					heaviestNode = n;
				}
			}
		}
		return heaviestNode;
	}

	/**
	 * States the problem as an ILP-Problem which is processed with the library
	 * 'LpSolve'.
	 * <p>
	 * Assuming an edge is 1 if its cut and 0 if not, and a node is 1 if marked and
	 * 0 if not, the constraints that every edge e_ij, and its end nodes v_i and v_j
	 * need to satisfy are: <blockquote>
	 *
	 * <pre>
	 *    e_ij - v_i - v_j <= 0 AND e_ij + v_i + v_j <= 2
	 * </pre>
	 *
	 * </blockquote> It follows that if both nodes are marked or unmarked, the edge
	 * cannot be cut. We maximize the sum of the weights of all cut edges.
	 * <p>
	 * This library implements the constraints as arrays of coefficients. To express
	 * a constraint such as the one above, we need an array with as many entries as
	 * there are edges plus nodes. An entry at position x+1 is 1 iff edge x is
	 * included in that constraint. Let e denote the total number of edges, then an
	 * entry at position e+y+1 is 1 iff vertex y is included in that constraint.
	 * <p>
	 * Additionally, the first entry of the coefficient array needs to be the number
	 * of variables (as stated by library documentation).
	 * <p>
	 * Assume we have the 3 edges e_12 e_13 and e_23 in this very order in a
	 * complete graph with 3 vertices. To express the constraint e_13 + v_1 + v_3 <=
	 * 2, we would need the following coefficient array: [6; 0, 1, 0; 1, 0, 1].
	 */
	public static void ILP(Graph graph) {
		int nodeCount = graph.nodeCount;
		int edgeCount = graph.edgeCount;
		int nodeEdgeCount = nodeCount + edgeCount;

		try {
			LpSolve solver = LpSolve.makeLp(0, nodeEdgeCount);

			solver.setVerbose(0); // Default is 4
			solver.setTimeout(3 * 60); // Terminate after 3 minutes

			// Set all variables to binary
			for (int i = 1; i <= nodeEdgeCount; i++) {
				solver.setBinary(i, true);
			}

			double[] constraint = new double[nodeEdgeCount + 1];

			for (int j = 0; j < edgeCount; j++) {

				constraint[0] = nodeEdgeCount;
				for (int i = 1; i <= nodeEdgeCount; i++) {
					constraint[i] = 0;
				}

				constraint[j + 1] = 1;
				constraint[edgeCount + graph.edgeList.get(j).node1.id] = 1;
				constraint[edgeCount + graph.edgeList.get(j).node2.id] = 1;

				solver.addConstraint(constraint, LpSolve.LE, 2);
			}
			for (int j = 0; j < edgeCount; j++) {

				constraint[0] = nodeEdgeCount;
				for (int i = 1; i <= nodeEdgeCount; i++) {
					constraint[i] = 0;
				}

				constraint[j + 1] = 1;
				constraint[edgeCount + graph.edgeList.get(j).node1.id] = -1;
				constraint[edgeCount + graph.edgeList.get(j).node2.id] = -1;

				solver.addConstraint(constraint, LpSolve.LE, 0);
			}

			double[] cost = new double[nodeEdgeCount + 1];
			cost[0] = nodeEdgeCount;
			for (int i = 1; i < (edgeCount + 1); i++) {
				// We set cost of all edges as the objective
				cost[i] = graph.edgeList.get(i - 1).weight;
			}
			for (int i = edgeCount + 1; i < (nodeEdgeCount + 1); i++) {
				// We don't care about which nodes are marked for the objective
				cost[i] = 0;
			}

			// We want to maximize, not minimize (default) the sum of the edge weights.
			solver.setMaxim();
			solver.setObjFn(cost);
			solver.solve();

			// We mark all nodes that were set to 1 as a byproduct of edge weight
			// maximization
			double[] var = solver.getPtrVariables();
			for (int i = 0; i < nodeCount; i++) {
				if (var[edgeCount + i] == 1.0) {
					graph.nodeList.get(i).mark();
				}
			}
		} catch (LpSolveException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Reads a graph from a text file in the following format:
	 * <p>
	 * {graph name} {number of vertices} {number of edges} {(optional)
	 * weighted?}<br>
	 * {edge1 nodeU} {edge1 nodeV} {(optional) weight} <br>
	 * {edge2 nodeU} {edge2 nodeV} {(optional) weight} <br>
	 * {edge3 nodeU} {edge3 nodeV} {(optional) weight} <br>
	 * ... <br>
	 * no newline
	 *
	 * @return Array with [graph name, number of vertices, number of edges]
	 */
	public static String[] read(String filename, Graph graph) {

		try {
			File file = new File(filename);
			Scanner s = new Scanner(file);
			String[] metadata = s.nextLine().split(" ");

			boolean weighted = (metadata.length == 3);

			for (int i = 1; i <= (Integer.parseInt(metadata[1])); i++) {
				graph.addNode(new Node(i));
			}

			while (s.hasNextLine()) {
				String[] data = s.nextLine().split(" ");
				int u = Integer.parseInt(data[1]);
				int v = Integer.parseInt(data[2]);

				// Adds unweighted edges with weight 1 if entire graph is unweighted
				graph.addEdge(weighted ? new Edge(graph, u, v, Integer.parseInt(data[3])) : new Edge(graph, u, v, 1));
			}

			s.close();
			return metadata;
		} catch (FileNotFoundException e) {
			System.out.println("Cannot read file");
			e.printStackTrace();
		}

		return null;

	}

	private static void results(Graph graph, long time) {
		System.out.println("Time: " + ((System.nanoTime() - time) / 1000) + " µs");

		printCutWeight(graph);

		if (graph.edgeList.size() < 20) {
			// Print the two node subsets for small graphs
			printSubsets(graph);
		}

		if (graph.edgeList.size() < 10) {
			// Explicitly name all of the cut edges + weights for tiny graphs
			printCutEdges(graph);
		}

	}

	private static void printCutWeight(Graph graph) {
		int weight = 0;

		for (Edge e : graph.edgeList) {
			if ((e.node1.marked && !e.node2.marked) || (e.node2.marked && !e.node1.marked)) {
				weight += e.weight;
			}
		}

		System.out.println("\n" + "Weight of cut edges: " + weight);
	}

	private static void printSubsets(Graph graph) {
		ArrayList<Node> unmarked = new ArrayList<>();
		ArrayList<Node> marked = new ArrayList<>();

		for (Node n : graph.nodeList) {
			if (n.marked) {
				marked.add(n);
			} else {
				unmarked.add(n);
			}
		}

		System.out.println("Set 1: " + marked);
		System.out.println("Set 2: " + unmarked);
	}

	private static void printCutEdges(Graph graph) {
		int counter = 0;

		System.out.print("Cut edges: ");

		for (Edge e : graph.edgeList) {
			if ((e.node1.marked && !e.node2.marked) || (e.node2.marked && !e.node1.marked)) {
				System.out.print(e + ",  ");
				counter++;
			}
		}

		System.out.println("Total: " + counter);
	}

}
