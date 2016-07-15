package bomberman;

import lombok.Builder;
import lombok.Data;

/**
 * TODO: Description
 *
 * @author gregor.weckbecker@gmail.com
 */
@Data
@Builder
public class PlayerScore {

    public final String playerName;
    public final int score;
    public final int positionX;
    public final int positionY;

    public Coordinate position() {
        return new Coordinate(positionX, positionY);
    }

}
