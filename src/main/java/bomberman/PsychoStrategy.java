package bomberman;

import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.List;

import static bomberman.RandomUtils.flip;
import static bomberman.RandomUtils.randomUpTo;

/**
 * TODO: Description
 *
 * @author gregor.weckbecker@gmail.com
 */
public class PsychoStrategy implements GameStrategy {

    static class MovementEdge extends DefaultWeightedEdge {
        String command;

        public MovementEdge(String command) {
            this.command = command;
        }
    }

    private GameState oldState;

    @Override
    public String choose(GameState state) {
        PlayerScore myScore = state.myScore();
        boolean isFox = state.isFox(myScore);

        if (isFox) {
            Integer targetX = state.getMapSizeX() - (int) state.getScores().stream()
                    .filter(player -> !player.equals(myScore))
                    .mapToInt(PlayerScore::getPositionX)
                    .average().getAsDouble();

            Integer targetY = state.getMapSizeY() - (int) state.getScores().stream()
                    .filter(player -> !player.equals(myScore))
                    .mapToInt(PlayerScore::getPositionY)
                    .average().getAsDouble();

            Coordinate target = getValidRandom(state, targetX, targetY);

            return move(state, target, isFox);
        }

        Coordinate target = state.foxPosition()
                .orElseGet(() -> getValidRandom(state, state.getMapSizeX() - myScore.positionX, state.getMapSizeY() - myScore.positionY));
        return move(state, target, isFox);
    }

    private String move(GameState state, Coordinate target, boolean isFox) {

        if (target.equals(state.myScore().position())) return "n";

        DefaultDirectedWeightedGraph<Coordinate, MovementEdge> graph = new DefaultDirectedWeightedGraph<>(MovementEdge.class);

        // add all vertexes
        for (int i = 0; i < state.getMapSizeX(); i++) {
            for (int j = 0; j < state.getMapSizeY(); j++) {
                String value = state.getMap().value(i, j);
                if (!value.equals("W") && !value.equals("w")) {
                    graph.addVertex(new Coordinate(i, j));
                }
            }
        }

        // add all edges
        for (int i = 0; i < state.getMapSizeX(); i++) {
            for (int j = 0; j < state.getMapSizeY(); j++) {
                String value = state.getMap().value(i, j);
                if (!value.equals("W") && !value.equals("w")) {
                    Coordinate start = coordinate(i, j);
                    createEdge(start, coordinate(i, j + 1), "s", state.getMap(), graph, isFox);
                    createEdge(start, coordinate(i, j - 1), "w", state.getMap(), graph, isFox);
                    createEdge(start, coordinate(i + 1, j), "d", state.getMap(), graph, isFox);
                    createEdge(start, coordinate(i - 1, j), "a", state.getMap(), graph, isFox);
                }
            }
        }


        BellmanFordShortestPath<Coordinate, MovementEdge> bellmanFordShortestPath = new BellmanFordShortestPath<>(graph, state.myScore().position());
        List<MovementEdge> pathEdgeList = bellmanFordShortestPath.getPathEdgeList(target);


        return pathEdgeList != null ? pathEdgeList.get(0).command : "n";
    }

    private Coordinate coordinate(int i, int j) {
        return new Coordinate(i, j);
    }

    private void createEdge(Coordinate start, Coordinate target, String move, GameMap map,
                            DefaultDirectedWeightedGraph<Coordinate, MovementEdge> graph, boolean isFox) {
        String value = map.value(target);
        if (value.equals("W") || value.equals("w")) return;
        if (value.equals("_")) addWeightedEdge(start, target, move, graph, 1.0);
        if (value.equals("h")) addWeightedEdge(start, target, move, graph, 0.5);
        if (value.equals("b")) addWeightedEdge(start, target, move, graph, 0.5);
        if (value.equals("f")) addWeightedEdge(start, target, move, graph, flip() ? 0.0 : 10.0);
        if (value.equals("p")) addWeightedEdge(start, target, move, graph, isFox && flip() ? 1000.0 : 1.0);
    }

    private void addWeightedEdge(Coordinate start, Coordinate target, String move, DefaultDirectedWeightedGraph<Coordinate, MovementEdge> graph, double weight) {
        MovementEdge movementEdge = new MovementEdge(move);
        graph.addEdge(start, target, movementEdge);
        graph.setEdgeWeight(movementEdge, weight);
    }

    private Coordinate getValidRandom(GameState state, Integer startX, Integer startY) {
        int targetX = startX;
        int targetY = startY;

        while (state.getMap().value(targetX, targetY).equals("w") || state.getMap().value(targetX, targetY).equals("W")) {
            targetX = randomUpTo(state.getMapSizeX());
            targetY = randomUpTo(state.getMapSizeY());
        }

        return coordinate(targetX, targetY);
    }

}
