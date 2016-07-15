package bomberman;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Optional;

/**
 * TODO: Description
 *
 * @author gregor.weckbecker@gmail.com
 */
@Data
@Builder
public class GameState {

    private final String playerName;

    private final int currentGameNumber;
    private final int totalGames;
    private final int roundNumber;
    private final int totalRounds;

    private final int playersCount;
    private final int mapSizeX;
    private final int mapSizeY;

    private final float timeout;

    private final GameMap map;

    private final List<PlayerScore> scores;

    public PlayerScore myScore() {
        return scores.stream().filter(score -> score.getPlayerName().equals(playerName)).findFirst().get();
    }

    public boolean isFox(PlayerScore player) {
        return map.value(player.position()).equals("f");
    }

    public Optional<Coordinate> foxPosition() {
        return scores.stream().filter(this::isFox).findFirst().map(PlayerScore::position);
    }
}
