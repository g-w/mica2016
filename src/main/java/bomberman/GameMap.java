package bomberman;

import lombok.Data;

/**
 * TODO: Description
 *
 * @author gregor.weckbecker@gmail.com
 */
@Data
public class GameMap {
    public final String mapString;
    public final Integer sizeX;
    public final Integer sizeY;

    public String value(Coordinate position) {
        return value(position.getX(), position.getY());
    }

    public String value(int x, int y) {
        int index = 2 * x + y * 2 * sizeX;
        return mapString.substring(index, index + 1);
    }
}
