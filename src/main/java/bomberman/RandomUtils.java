package bomberman;

import java.util.List;
import java.util.Random;

import static java.util.Arrays.asList;

/**
 * TODO: Description
 *
 * @author gregor.weckbecker@gmail.com
 */
public class RandomUtils {
    private static final Random RANDOM_SOURCE = new Random(System.currentTimeMillis());

    public static boolean flip() {
        return RANDOM_SOURCE.nextBoolean();
    }

    public static int randomUpTo(int i) {
        int value = RANDOM_SOURCE.nextInt();
        return (Integer.signum(value) * value) % i;
    }

    public static <T> T choice(T... list) {
        return choice(asList(list));
    }

    public static <T> T choice(List<T> list) {
        return list.get(RANDOM_SOURCE.nextInt(list.size()));
    }
}
