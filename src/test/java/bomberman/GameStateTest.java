package bomberman;

import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * TODO: Description
 *
 * @author Codevise Solutions Ltd.
 *         info@codevise.de
 */
public class GameStateTest {

    static class MyEdge extends DefaultWeightedEdge {
        Integer id;

        public MyEdge(Integer id) {
            this.id = id;
        }

        void setWeight(double w) {

        }
    }

    @Test
    public void testGraph() {
        DefaultDirectedWeightedGraph<String, MyEdge> graph = new DefaultDirectedWeightedGraph<>(MyEdge.class);

        graph.addVertex("A");
        graph.addVertex("B");
        graph.addVertex("C");
        graph.addVertex("D");


        MyEdge start = addEdge(graph, "A", "B", 1, 1);
        addEdge(graph, "B", "C", 2, 1);
        MyEdge target = addEdge(graph, "A", "D", 3, 2);
        addEdge(graph, "D", "C", 4, -1.5);

        List<MyEdge> pathEdgeList = new BellmanFordShortestPath<>(graph, "A").getPathEdgeList("C");

        assertEquals(3, (int) pathEdgeList.get(0).id);
        assertEquals(4, (int) pathEdgeList.get(1).id);
    }

    private MyEdge addEdge(DefaultDirectedWeightedGraph<String, MyEdge> graph, String source, String target, int id, double weight) {
        MyEdge edge = createEdge(id);
        graph.addEdge(source, target, edge);
        graph.setEdgeWeight(edge, weight);
        return edge;
    }

    private MyEdge createEdge(int id) {
        return new MyEdge(id);
    }

}