package bomberman;

import lombok.SneakyThrows;
import org.apache.commons.net.telnet.TelnetClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.unmodifiableList;

/**
 * TODO: Description
 *
 * @author gregor.weckbecker@gmail.com
 */
public class GameClient {
    private static final Pattern GAME_STATE_PATTERN =
            Pattern.compile("game:([0-9]+)/([0-9]+),round:([0-9]+)/([0-9]+),players:([0-9]+),mapsize:x([0-9]+)y([0-9]+),timeout:([0-9]+\\.[0-9]+).*");
    private static final Pattern SCORELINE_PATTERN =
            Pattern.compile("name:(.+),score:([0-9]+),x:([0-9]+),y:([0-9]+);.*");


    private final String name;
    private final GameStrategy strategy;

    private final BufferedReader in;
    private final PrintWriter out;

    @SneakyThrows
    public GameClient(String hostname, int port, String name, GameStrategy strategy) {
        this.name = name;
        this.strategy = strategy;

        TelnetClient telnet = new TelnetClient();
        telnet.connect(hostname, port);
        in = new BufferedReader(new InputStreamReader(telnet.getInputStream()));
        out = new PrintWriter(telnet.getOutputStream());
    }

    @SneakyThrows
    public void run() {
        out.println("name:" + name);
        out.flush();

        boolean ongoing = true;
        boolean readingMap = false;
        String mapString = "";
        boolean readingScoretable = false;
        List<PlayerScore> scores = new ArrayList<>();

        GameState.GameStateBuilder builder = GameState.builder();
        builder.playerName(name);

        Integer mapSizeX = 0, mapSizeY = 0;

        while (ongoing) {
            String line = in.readLine();

            if (line == null) {
                Thread.sleep(1);
                continue;
            }

            line = line.trim();

            if (line.startsWith("game:")) {
                builder = GameState.builder();
                builder.playerName(name);

                Matcher gameMatch = GAME_STATE_PATTERN.matcher(line);
                gameMatch.matches();

                mapSizeX = Integer.parseInt(gameMatch.group(6));
                mapSizeY = Integer.parseInt(gameMatch.group(7));

                builder
                        .currentGameNumber(Integer.parseInt(gameMatch.group(1)))
                        .totalGames(Integer.parseInt(gameMatch.group(2)))
                        .roundNumber(Integer.parseInt(gameMatch.group(3)))
                        .totalRounds(Integer.parseInt(gameMatch.group(4)))
                        .playersCount(Integer.parseInt(gameMatch.group(5)))
                        .mapSizeX(mapSizeX)
                        .mapSizeY(mapSizeY)
                        .timeout(Float.parseFloat(gameMatch.group(8)));

            } else if (line.startsWith("map:")) {
                readingMap = true;
            } else if (readingMap && line.isEmpty()) {
                readingMap = false;
                builder.map(new GameMap(mapString, mapSizeX, mapSizeY));
                mapString = "";
            } else if (readingMap) {
                mapString += line.trim();
            } else if (line.startsWith("scoretable:")) {
                scores = new ArrayList<>();
                readingScoretable = true;
            } else if (line.startsWith("/scoretable")) {
                builder.scores(unmodifiableList(scores));
                readingScoretable = false;
            } else if (readingScoretable) {
                Matcher matcher = SCORELINE_PATTERN.matcher(line);
                matcher.matches();

                PlayerScore.PlayerScoreBuilder scoreBuilder = PlayerScore.builder()
                        .playerName(matcher.group(1))
                        .score(Integer.parseInt(matcher.group(2)))
                        .positionX(Integer.parseInt(matcher.group(3)))
                        .positionY(Integer.parseInt(matcher.group(4)));

                scores.add(scoreBuilder.build());

            } else if (line.startsWith("wfyc")) {
                GameState gameState = builder.build();

                String choice = strategy.choose(gameState);
                out.println(choice + '\n');
                out.flush();

                System.out.println(name + ": my choice is " + choice);

            } else if (line.startsWith("game is over")) {
                ongoing = false;
                System.out.println("Good Bye!");
            }
        }

    }

}
