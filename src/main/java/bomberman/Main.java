package bomberman;

import java.io.IOException;
import java.util.List;

import static bomberman.RandomUtils.choice;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * TODO: Description
 *
 * @author gregor.weckbecker@gmail.com
 */
public class Main {
    private static final List<String> FIRST_NAMES = unmodifiableList(asList("Graham", "John", "Terry", "Eric", "Terry", "Michael"));
    private static final List<String> SECOND_NAMES = unmodifiableList(asList("Chapman", "Cleese", "Gilliam", "Idle", "Jones", "Palin"));

    private static GameClient client;

    public static void main(String[] args) throws IOException, InterruptedException {
        String name = choice(FIRST_NAMES) + " " + choice(SECOND_NAMES);
        System.out.println("Hello my name is " + name);

        client = new GameClient("127.0.0.1", 5000, name, new PsychoStrategy());
        client.run();
    }

}
