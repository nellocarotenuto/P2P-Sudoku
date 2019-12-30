package com.github.nellocarotenuto.p2psudoku.gui;

import com.github.nellocarotenuto.p2psudoku.challenge.*;
import com.github.nellocarotenuto.p2psudoku.sudoku.FilledCellException;
import com.github.nellocarotenuto.p2psudoku.sudoku.FixedCellException;
import com.github.nellocarotenuto.p2psudoku.sudoku.InvalidNumberException;
import com.github.nellocarotenuto.p2psudoku.sudoku.Sudoku;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.Symbols;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import net.tomp2p.utils.Pair;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.Properties;

public class Console {

    private static final String[] LABELS = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o",
            "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};

    private Terminal terminal;
    private Screen screen;
    private TextGraphics textGraphics;

    private Properties properties;

    private GameClient client;

    @Option(name="-ma", aliases="--master-address", usage="The address of the master peer")
    private String masterPeerAddress = "127.0.0.1";

    @Option(name="-mp", aliases="--master-port", usage="The port of the master peer")
    private int masterPeerPort = GameClientImpl.DEFAULT_PORT;

    @Option(name="-lp", aliases="--local-port", usage="The local port to use to connect")
    private int localPort = GameClientImpl.DEFAULT_PORT;

    public static void main(String[] args) {
        Console console = null;

        try {
            console = new Console(args);
            console.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (console != null) {
                try {
                    console.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Builds a new console backed by a client.
     *
     * @param args the array of connection arguments to be parsed
     */
    private Console(String[] args) throws Exception {
        // Parse arguments
        try {
            CmdLineParser parser = new CmdLineParser(this);
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            e.printStackTrace();
            return;
        }

        // Create a client and show login
        client = new GameClientImpl(InetAddress.getByName(masterPeerAddress), masterPeerPort, localPort);

        // Load project resources
        try (InputStream input = ClassLoader.getSystemResourceAsStream("strings.properties")) {

            if (input == null) {
                System.err.println("Error: unable to locate resources.");
                System.exit(1);
            }

            properties = new Properties();
            properties.load(input);
        } catch (IOException e) {
            System.err.println("Error: unable to load resources.");
            return;
        }

        // Set up GUI components
        try {
            terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            textGraphics = screen.newTextGraphics();

            screen.startScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows the login screen.
     */
    private void showLogin() throws Exception {
        TextColor messageColor = TextColor.ANSI.DEFAULT;
        String message = properties.getProperty("login.tips.1");

        StringBuilder command = new StringBuilder();

        screen.clear();

        while (true) {
            screen.doResizeIfNecessary();

            // Header
            TerminalPosition topLeft = new TerminalPosition(1, 0);

            textGraphics.setCharacter(topLeft,
                    Symbols.DOUBLE_LINE_TOP_LEFT_CORNER);

            textGraphics.drawLine(topLeft.withRelativeColumn(1),
                    topLeft.withRelativeColumn(76),
                    Symbols.DOUBLE_LINE_HORIZONTAL);

            textGraphics.setCharacter(topLeft.withRelativeColumn(77),
                    Symbols.DOUBLE_LINE_TOP_RIGHT_CORNER);

            textGraphics.setCharacter(topLeft.withRelativeRow(1),
                    Symbols.DOUBLE_LINE_VERTICAL);

            textGraphics.putString(topLeft.withRelativeRow(1).withRelativeColumn(2),
                    String.format("%s", properties.getProperty("login.header")),
                    SGR.BOLD);

            textGraphics.setCharacter(topLeft.withRelativeRow(1).withRelativeColumn(77),
                    Symbols.DOUBLE_LINE_VERTICAL);

            textGraphics.setCharacter(topLeft.withRelativeRow(2),
                    Symbols.DOUBLE_LINE_BOTTOM_LEFT_CORNER);

            textGraphics.drawLine(topLeft.withRelativeRow(2).withRelativeColumn(1),
                    topLeft.withRelativeRow(2).withRelativeColumn(76),
                    Symbols.DOUBLE_LINE_HORIZONTAL);

            textGraphics.setCharacter(topLeft.withRelativeRow(2).withRelativeColumn(77),
                    Symbols.DOUBLE_LINE_BOTTOM_RIGHT_CORNER);

            // Description
            textGraphics.putString(topLeft.withRelativeRow(4).withRelativeColumn(1),
                    String.format("%s", properties.getProperty("login.welcome")));

            textGraphics.putString(topLeft.withRelativeRow(6).withRelativeColumn(1),
                    String.format("%s", properties.getProperty("login.description.1")));

            textGraphics.putString(topLeft.withRelativeRow(7).withRelativeColumn(1),
                    String.format("%s", properties.getProperty("login.description.2")));

            // Nickname
            textGraphics.drawLine(topLeft.withRelativeRow(9).withRelativeColumn(1),
                    topLeft.withRelativeRow(9).withRelativeColumn(76),
                    Symbols.DOUBLE_LINE_HORIZONTAL);

            textGraphics.putString(topLeft.withRelativeRow(11).withRelativeColumn(1),
                    String.format("%s", properties.getProperty("login.nickname.choice")));

            textGraphics.putString(topLeft.withRelativeRow(13).withRelativeColumn(1),
                    ">", SGR.BOLD);

            if (command.length() == 0) {
                screen.setCursorPosition(topLeft.withRelativeRow(13).withRelativeColumn(3));
            }

            textGraphics.setForegroundColor(messageColor);

            textGraphics.putString(topLeft.withRelativeRow(15).withRelativeColumn(1),
                    message);

            textGraphics.setForegroundColor(TextColor.ANSI.DEFAULT);

            screen.refresh();

            // Input handling
            KeyStroke keyStroke = screen.pollInput();

            if (keyStroke == null) {
                Thread.sleep(30);
                continue;
            }

            if (keyStroke.getKeyType() == KeyType.Enter) {
                textGraphics.putString(topLeft.withRelativeRow(13).withRelativeColumn(3),
                        String.format("%743s", ""));

                screen.setCursorPosition(topLeft.withRelativeRow(13).withRelativeColumn(3));

                String input = command.toString().trim();

                if (input.toLowerCase().equals("!quit")) {
                    return;
                } else {
                    try {
                        client.login(input);
                        showChallengesList();
                        return;
                    } catch (InvalidNicknameException e) {
                        messageColor = TextColor.ANSI.RED;
                        message = String.format("%-78s", properties.getProperty("login.nickname.invalid"));
                    } catch (TakenNicknameException e) {
                        messageColor = TextColor.ANSI.YELLOW;
                        message = String.format("%-78s", properties.getProperty("login.nickname.taken"));
                    }
                }

                command = new StringBuilder();
            } else if (keyStroke.getKeyType() == KeyType.Backspace) {
                if (command.length() == 0) {
                    continue;
                }

                command.deleteCharAt(command.length() - 1);

                textGraphics.putString(topLeft.withRelativeRow(13).withRelativeColumn(3),
                        String.format("%-78s", command.toString()), SGR.BOLD);

                screen.setCursorPosition(topLeft.withRelativeRow(13).withRelativeColumn(3 + command.length()));
            } else if (keyStroke.getKeyType() == KeyType.Character) {
                if (command.length() == 24) {
                    continue;
                }

                command.append(keyStroke.getCharacter());

                textGraphics.putString(topLeft.withRelativeRow(13).withRelativeColumn(3),
                        command.toString(),
                        SGR.BOLD);

                screen.setCursorPosition(topLeft.withRelativeRow(13).withRelativeColumn(3 + command.length()));
            }
        }

    }

    /**
     * Shows the list of public challenges.
     */
    private void showChallengesList() throws Exception {
        TextColor messageColor = TextColor.ANSI.DEFAULT;
        String message = properties.getProperty("list.tips.3");

        StringBuilder command = new StringBuilder();

        screen.clear();

        while (true) {
            screen.doResizeIfNecessary();

            // Header
            TerminalPosition topLeft = new TerminalPosition(1, 0);

            textGraphics.setCharacter(topLeft,
                    Symbols.DOUBLE_LINE_TOP_LEFT_CORNER);

            textGraphics.drawLine(topLeft.withRelativeColumn(1),
                    topLeft.withRelativeColumn(76),
                    Symbols.DOUBLE_LINE_HORIZONTAL);

            textGraphics.setCharacter(topLeft.withRelativeColumn(77),
                    Symbols.DOUBLE_LINE_TOP_RIGHT_CORNER);

            textGraphics.setCharacter(topLeft.withRelativeRow(1),
                    Symbols.DOUBLE_LINE_VERTICAL);

            textGraphics.putString(topLeft.withRelativeRow(1).withRelativeColumn(2),
                    String.format("%s", properties.getProperty("list.header")),
                    SGR.BOLD);

            textGraphics.setCharacter(topLeft.withRelativeRow(1).withRelativeColumn(77),
                    Symbols.DOUBLE_LINE_VERTICAL);

            textGraphics.setCharacter(topLeft.withRelativeRow(2),
                    Symbols.DOUBLE_LINE_BOTTOM_LEFT_CORNER);

            textGraphics.drawLine(topLeft.withRelativeRow(2).withRelativeColumn(1),
                    topLeft.withRelativeRow(2).withRelativeColumn(76),
                    Symbols.DOUBLE_LINE_HORIZONTAL);

            textGraphics.setCharacter(topLeft.withRelativeRow(2).withRelativeColumn(77),
                    Symbols.DOUBLE_LINE_BOTTOM_RIGHT_CORNER);

            // List
            textGraphics.putString(topLeft.withRelativeRow(4).withRelativeColumn(1),
                    String.format("%-28s%-28s%10s%10s",
                            properties.getProperty("list.names"),
                            properties.getProperty("list.owner"),
                            properties.getProperty("list.status"),
                            properties.getProperty("list.players")));

            textGraphics.drawLine(topLeft.withRelativeRow(5).withRelativeColumn(1),
                    topLeft.withRelativeRow(5).withRelativeColumn(76),
                    Symbols.DOUBLE_LINE_HORIZONTAL);

            List<ChallengeInfo> challengesList = client.listChallenges();

            for (int row = 0; row < 10; row++) {
                if (row < challengesList.size()) {
                    ChallengeInfo challenge = challengesList.get(row);

                    textGraphics.putString(topLeft.withRelativeRow(6 + row).withRelativeColumn(1),
                            String.format("%-28s%-28s", challenge.getName(), challenge.getOwner()));

                    if (challenge.getStatus() == ChallengeStatus.WAITING) {
                        textGraphics.setForegroundColor(TextColor.ANSI.YELLOW);
                        textGraphics.putString(topLeft.withRelativeRow(6 + row).withRelativeColumn(1 + 56),
                                String.format("%10s", properties.getProperty("challenge.status.waiting")));
                    } else if (challenge.getStatus() == ChallengeStatus.PLAYING) {
                        textGraphics.setForegroundColor(TextColor.ANSI.GREEN);
                        textGraphics.putString(topLeft.withRelativeRow(6 + row).withRelativeColumn(1 + 56),
                                String.format("%10s", properties.getProperty("challenge.status.playing")));
                    } else if (challenge.getStatus() == ChallengeStatus.ENDED) {
                        textGraphics.setForegroundColor(TextColor.ANSI.BLUE);
                        textGraphics.putString(topLeft.withRelativeRow(6 + row).withRelativeColumn(1 + 56),
                                String.format("%10s", properties.getProperty("challenge.status.ended")));
                    }

                    textGraphics.setForegroundColor(TextColor.ANSI.DEFAULT);
                    textGraphics.putString(topLeft.withRelativeRow(6 + row).withRelativeColumn(1 + 66),
                            String.format("%10s", challenge.getPlayers()));
                } else {
                    textGraphics.putString(topLeft.withRelativeRow(6 + row).withRelativeColumn(1),
                            String.format("%-76s", ""));
                }
            }

            // Command area
            textGraphics.drawLine(topLeft.withRelativeRow(17).withRelativeColumn(1),
                    topLeft.withRelativeRow(17).withRelativeColumn(76),
                    Symbols.DOUBLE_LINE_HORIZONTAL);

            textGraphics.putString(topLeft.withRelativeRow(18).withRelativeColumn(1),
                    String.format("%-78s", properties.getProperty("list.tips.1")));

            textGraphics.putString(topLeft.withRelativeRow(19).withRelativeColumn(1),
                    String.format("%-78s", properties.getProperty("list.tips.2")));

            textGraphics.putString(topLeft.withRelativeRow(21).withRelativeColumn(1),
                    ">", SGR.BOLD);

            if (command.length() == 0) {
                screen.setCursorPosition(topLeft.withRelativeRow(21).withRelativeColumn(3));
            }

            textGraphics.setForegroundColor(messageColor);

            textGraphics.putString(topLeft.withRelativeRow(23).withRelativeColumn(1),
                    message);

            textGraphics.setForegroundColor(TextColor.ANSI.DEFAULT);

            screen.refresh();

            // Input handling
            KeyStroke keyStroke = screen.pollInput();

            if (keyStroke == null) {
                Thread.sleep(30);
                continue;
            }

            if (keyStroke.getKeyType() == KeyType.Enter) {
                textGraphics.putString(topLeft.withRelativeRow(21).withRelativeColumn(3),
                        String.format("%73s", ""));

                screen.setCursorPosition(topLeft.withRelativeRow(21).withRelativeColumn(3));

                String input = command.toString().trim();

                if (input.equals("!quit")) {
                    return;
                } else if (input.startsWith("!join")) {
                    input = input.substring("!join".length()).trim();

                    try {
                        client.joinChallenge(input);
                        showChallenge();
                        screen.clear();
                    } catch (ChallengeNotFoundException e) {
                        messageColor = TextColor.ANSI.RED;
                        message = String.format("%-76s", properties.getProperty("list.messages.notfound"));
                    }
                } else if (input.startsWith("!public") || input.startsWith("!private")) {
                    boolean listed;

                    if (input.startsWith("!public")) {
                        input = input.substring("!public".length()).trim();
                        listed = true;
                    } else {
                        input = input.substring("!private".length()).trim();
                        listed = false;
                    }

                    try {
                        client.createChallenge(input, (int) System.currentTimeMillis(), listed);
                        showChallenge();
                        screen.clear();
                        message = String.format("%-76s", "");
                    } catch (ChallengeAlreadyExistsException e) {
                        messageColor = TextColor.ANSI.RED;
                        message = String.format("%-76s", properties.getProperty("list.messages.name.exists"));
                    } catch (InvalidChallengeNameException e) {
                        messageColor = TextColor.ANSI.RED;
                        message = String.format("%-76s", properties.getProperty("list.messages.name.invalid"));
                    }
                } else {
                    messageColor = TextColor.ANSI.RED;
                    message = String.format("%-76s", properties.getProperty("list.messages.invalid"));
                }

                command = new StringBuilder();
            } else if (keyStroke.getKeyType() == KeyType.Backspace) {
                if (command.length() == 0) {
                    continue;
                }

                command.deleteCharAt(command.length() - 1);

                textGraphics.putString(topLeft.withRelativeRow(21).withRelativeColumn(3),
                        String.format("%-73s", command.toString()), SGR.BOLD);

                screen.setCursorPosition(topLeft.withRelativeRow(21).withRelativeColumn(3 + command.length()));
            } else if (keyStroke.getKeyType() == KeyType.Character) {
                if (command.length() == 73) {
                    continue;
                }

                command.append(keyStroke.getCharacter());

                textGraphics.putString(topLeft.withRelativeRow(21).withRelativeColumn(3),
                        command.toString(),
                        SGR.BOLD);

                screen.setCursorPosition(topLeft.withRelativeRow(21).withRelativeColumn(3 + command.length()));
            }
        }
    }

    /**
     * Shows the current challenge.
     */
    private void showChallenge() throws Exception {
        TextColor messageColor = TextColor.ANSI.DEFAULT;
        String message = properties.getProperty("challenge.tips.3");

        StringBuilder command = client.isChallengeOwner() ? new StringBuilder("!start") : new StringBuilder();

        screen.clear();

        while (true) {
            screen.doResizeIfNecessary();

            // Header
            TerminalPosition topLeft = new TerminalPosition(1, 0);

            textGraphics.setCharacter(topLeft,
                    Symbols.DOUBLE_LINE_TOP_LEFT_CORNER);

            textGraphics.drawLine(topLeft.withRelativeColumn(1),
                    topLeft.withRelativeColumn(76),
                    Symbols.DOUBLE_LINE_HORIZONTAL);

            textGraphics.setCharacter(topLeft.withRelativeColumn(77),
                    Symbols.DOUBLE_LINE_TOP_RIGHT_CORNER);

            textGraphics.setCharacter(topLeft.withRelativeRow(1),
                    Symbols.DOUBLE_LINE_VERTICAL);

            textGraphics.putString(topLeft.withRelativeRow(1).withRelativeColumn(2),
                    String.format("%s", client.getChallengeName()),
                    SGR.BOLD);

            if (client.getChallengeStatus() == ChallengeStatus.WAITING) {
                textGraphics.setForegroundColor(TextColor.ANSI.YELLOW);

                textGraphics.putString(topLeft.withRelativeRow(1).withRelativeColumn(52),
                        String.format("%24s", properties.getProperty("challenge.status.waiting")));

                textGraphics.setForegroundColor(TextColor.ANSI.DEFAULT);
            } else if (client.getChallengeStatus() == ChallengeStatus.PLAYING) {
                textGraphics.setForegroundColor(TextColor.ANSI.GREEN);

                textGraphics.putString(topLeft.withRelativeRow(1).withRelativeColumn(52),
                        String.format("%24s", properties.getProperty("challenge.status.playing")));

                textGraphics.setForegroundColor(TextColor.ANSI.DEFAULT);
            } else if (client.getChallengeStatus() == ChallengeStatus.ENDED) {
                textGraphics.setForegroundColor(TextColor.ANSI.BLUE);

                textGraphics.putString(topLeft.withRelativeRow(1).withRelativeColumn(52),
                        String.format("%24s", properties.getProperty("challenge.status.ended")));

                textGraphics.setForegroundColor(TextColor.ANSI.DEFAULT);
            }

            textGraphics.setCharacter(topLeft.withRelativeRow(1).withRelativeColumn(77),
                    Symbols.DOUBLE_LINE_VERTICAL);

            textGraphics.setCharacter(topLeft.withRelativeRow(2),
                    Symbols.DOUBLE_LINE_BOTTOM_LEFT_CORNER);

            textGraphics.drawLine(topLeft.withRelativeRow(2).withRelativeColumn(1),
                    topLeft.withRelativeRow(2).withRelativeColumn(76),
                    Symbols.DOUBLE_LINE_HORIZONTAL);

            textGraphics.setCharacter(topLeft.withRelativeRow(2).withRelativeColumn(77),
                    Symbols.DOUBLE_LINE_BOTTOM_RIGHT_CORNER);

            // Board
            Integer[][] matrix = client.getChallengeBoard();
            StringBuilder line = new StringBuilder();

            for (int column = 0; column < Sudoku.SIDE_SIZE; column++) {

                if (column == 0) {
                    line.append(String.format("%4s", ""));
                }

                line.append(String.format(" %s  ", LABELS[column]));

                if (column == Sudoku.SIDE_SIZE - 1) {
                    textGraphics.putString(topLeft.withRelativeRow(4), line.toString());
                    line.delete(0, line.length());
                }
            }

            for (int column = 0; column < Sudoku.SIDE_SIZE; column++) {
                if (column == 0) {
                    line.append(String.format("%4s", Symbols.DOUBLE_LINE_TOP_LEFT_CORNER));
                }

                line.append(Symbols.DOUBLE_LINE_HORIZONTAL);
                line.append(Symbols.DOUBLE_LINE_HORIZONTAL);
                line.append(Symbols.DOUBLE_LINE_HORIZONTAL);

                if (column == Sudoku.SIDE_SIZE - 1) {
                    line.append(Symbols.DOUBLE_LINE_TOP_RIGHT_CORNER);
                    textGraphics.putString(topLeft.withRelativeRow(5), line.toString());
                    line.delete(0, line.length());
                } else if ((column + 1) % Sudoku.REGION_SIZE == 0) {
                    line.append(Symbols.DOUBLE_LINE_T_DOWN);
                } else {
                    line.append(Symbols.DOUBLE_LINE_T_SINGLE_DOWN);
                }
            }

            for (int row = 0; row < Sudoku.SIDE_SIZE; row++) {
                for (int column = 0; column < Sudoku.SIDE_SIZE; column++) {
                    if (column == 0) {
                        line.append(String.format("%2s %c", LABELS[row], Symbols.DOUBLE_LINE_VERTICAL));
                    }

                    String number;

                    if (client.getChallengeStatus() != ChallengeStatus.WAITING && matrix[row][column] != 0) {
                        number = matrix[row][column].toString();
                    } else {
                        number = " ";
                    }

                    line.append(String.format(" %-2s", number));

                    if (column == Sudoku.SIDE_SIZE - 1) {
                        line.append(Symbols.DOUBLE_LINE_VERTICAL);
                        textGraphics.putString(topLeft.withRelativeRow(6 + (2 * row)), line.toString());
                        line.delete(0, line.length());
                    } else if ((column + 1) % Sudoku.REGION_SIZE == 0) {
                        line.append(Symbols.DOUBLE_LINE_VERTICAL);
                    } else {
                        line.append(Symbols.SINGLE_LINE_VERTICAL);
                    }
                }

                if ((row + 1) % Sudoku.REGION_SIZE == 0 && row != Sudoku.SIDE_SIZE - 1) {
                    for (int column = 0; column < Sudoku.SIDE_SIZE; column++) {
                        if (column == 0) {
                            line.append(String.format("%4s", Symbols.DOUBLE_LINE_T_RIGHT));
                        }

                        line.append(Symbols.DOUBLE_LINE_HORIZONTAL);
                        line.append(Symbols.DOUBLE_LINE_HORIZONTAL);
                        line.append(Symbols.DOUBLE_LINE_HORIZONTAL);

                        if (column == Sudoku.SIDE_SIZE - 1) {
                            line.append(Symbols.DOUBLE_LINE_T_LEFT);
                            textGraphics.putString(topLeft.withRelativeRow(6 + (2 * row + 1)), line.toString());
                            line.delete(0, line.length());
                        } else if ((column + 1) % Sudoku.REGION_SIZE == 0) {
                            line.append(Symbols.DOUBLE_LINE_CROSS);
                        } else {
                            line.append(Symbols.DOUBLE_LINE_HORIZONTAL_SINGLE_LINE_CROSS);
                        }

                    }
                } else if (row != Sudoku.SIDE_SIZE - 1) {
                    for (int column = 0; column < Sudoku.SIDE_SIZE; column++) {
                        if (column == 0) {
                            line.append(String.format("%4s", Symbols.DOUBLE_LINE_T_SINGLE_RIGHT));
                        }

                        line.append(Symbols.SINGLE_LINE_HORIZONTAL);
                        line.append(Symbols.SINGLE_LINE_HORIZONTAL);
                        line.append(Symbols.SINGLE_LINE_HORIZONTAL);

                        if (column == Sudoku.SIDE_SIZE - 1) {
                            line.append(Symbols.DOUBLE_LINE_T_SINGLE_LEFT);
                            textGraphics.putString(topLeft.withRelativeRow(6 + (2 * row + 1)), line.toString());
                            line.delete(0, line.length());
                        } else if ((column + 1) % Sudoku.REGION_SIZE == 0) {
                            line.append(Symbols.DOUBLE_LINE_VERTICAL_SINGLE_LINE_CROSS);
                        } else {
                            line.append(Symbols.SINGLE_LINE_CROSS);
                        }

                    }
                }
            }

            for (int column = 0; column < Sudoku.SIDE_SIZE; column++) {
                if (column == 0) {
                    line.append(String.format("%4s", Symbols.DOUBLE_LINE_BOTTOM_LEFT_CORNER));
                }

                line.append(Symbols.DOUBLE_LINE_HORIZONTAL);
                line.append(Symbols.DOUBLE_LINE_HORIZONTAL);
                line.append(Symbols.DOUBLE_LINE_HORIZONTAL);

                if (column == Sudoku.SIDE_SIZE - 1) {
                    line.append(Symbols.DOUBLE_LINE_BOTTOM_RIGHT_CORNER);
                    textGraphics.putString(topLeft.withRelativeRow(23), line.toString());
                    line.delete(0, line.length());
                } else if ((column + 1) % Sudoku.REGION_SIZE == 0) {
                    line.append(Symbols.DOUBLE_LINE_T_UP);
                } else {
                    line.append(Symbols.DOUBLE_LINE_T_SINGLE_UP);
                }

            }

            // Scoreboard
            TerminalPosition boardTopRightEdge = topLeft.withRelativeRow(4).withRelativeColumn(4 * (Sudoku.SIDE_SIZE + 1) + 2);

            textGraphics.putString(boardTopRightEdge,
                    String.format("%-30s%5s",
                            properties.getProperty("challenge.scoreboard.players"),
                            properties.getProperty("challenge.scoreboard.scores")));

            textGraphics.drawLine(boardTopRightEdge.withRelativeRow(1),
                    boardTopRightEdge.withRelativeRow(1).withRelativeColumn(34),
                    Symbols.DOUBLE_LINE_HORIZONTAL);

            List<Pair<String, Integer>> scores = client.getChallengeScores();

            for (int player = 0; player < 10; player++) {
                if (player < scores.size()) {
                    Pair<String, Integer> score = scores.get(player);

                    if (score.element0().equals(client.getNickname())) {
                        textGraphics.putString(boardTopRightEdge.withRelativeRow(2 + player),
                                String.format("%-30s%5s", score.element0(), score.element1()),
                                SGR.BOLD);
                    } else {
                        textGraphics.putString(boardTopRightEdge.withRelativeRow(2 + player),
                                String.format("%-30s%5s", score.element0(), score.element1()));
                    }
                } else {
                    textGraphics.putString(boardTopRightEdge.withRelativeRow(2 + player),
                            String.format("%-35s", ""));
                }
            }

            // Input area
            TerminalPosition boardBottomRightEdge = topLeft.withRelativeRow(3 + 2 * (Sudoku.SIDE_SIZE + 1))
                    .withRelativeColumn(4 * (Sudoku.SIDE_SIZE + 1) + 2);

            textGraphics.drawLine(boardBottomRightEdge.withRelativeRow(-6),
                    boardBottomRightEdge.withRelativeRow(-6).withRelativeColumn(34),
                    Symbols.DOUBLE_LINE_HORIZONTAL);

            if (client.getChallengeStatus() == ChallengeStatus.WAITING) {
                if (client.isChallengeOwner()) {
                    textGraphics.putString(boardBottomRightEdge.withRelativeRow(-5),
                            String.format("%-35s", properties.getProperty("challenge.tips.4")));

                    textGraphics.putString(boardBottomRightEdge.withRelativeRow(-4),
                            String.format("%-35s", properties.getProperty("challenge.tips.5")));
                } else {
                    textGraphics.putString(boardBottomRightEdge.withRelativeRow(-5),
                            String.format("%-35s", properties.getProperty("challenge.tips.6")));

                    textGraphics.putString(boardBottomRightEdge.withRelativeRow(-4),
                            String.format("%-35s", properties.getProperty("challenge.tips.7")));
                }
            } else if (client.getChallengeStatus() == ChallengeStatus.PLAYING) {
                textGraphics.putString(boardBottomRightEdge.withRelativeRow(-5),
                        String.format("%-35s", properties.getProperty("challenge.tips.1")));

                textGraphics.putString(boardBottomRightEdge.withRelativeRow(-4),
                        String.format("%-35s", properties.getProperty("challenge.tips.2")));
            } else if (client.getChallengeStatus() == ChallengeStatus.ENDED) {
                textGraphics.putString(boardBottomRightEdge.withRelativeRow(-5),
                        String.format("%-35s", properties.getProperty("challenge.tips.8")));

                textGraphics.putString(boardBottomRightEdge.withRelativeRow(-4),
                        String.format("%-35s", properties.getProperty("challenge.tips.9")));
            }


            textGraphics.putString(boardBottomRightEdge.withRelativeRow(-2),
                    ">", SGR.BOLD);

            textGraphics.setForegroundColor(messageColor);

            textGraphics.putString(boardBottomRightEdge,
                    message);

            textGraphics.setForegroundColor(TextColor.ANSI.DEFAULT);

            if (command.length() == 0) {
                screen.setCursorPosition(boardBottomRightEdge.withRelativeRow(-2).withRelativeColumn(2));
            } else {
                screen.setCursorPosition(boardBottomRightEdge.withRelativeRow(-2).withRelativeColumn(2 + command.length()));
                textGraphics.putString(boardBottomRightEdge.withRelativeRow(-2).withRelativeColumn(2),
                        String.format("%-33s", command),
                        SGR.BOLD);
            }

            screen.refresh();

            // Input handling
            KeyStroke keyStroke = screen.pollInput();

            if (keyStroke == null) {
                Thread.sleep(30);
                continue;
            }

            if (keyStroke.getKeyType() == KeyType.Enter) {
                textGraphics.putString(boardBottomRightEdge.withRelativeRow(-2),
                        String.format("%35s", ""));

                screen.setCursorPosition(boardBottomRightEdge.withRelativeRow(-2).withRelativeColumn(2));

                boolean valid = true;
                String input = command.toString().trim().toLowerCase();

                if (input.equals("!start")) {
                    if (client.getChallengeStatus() != ChallengeStatus.WAITING) {
                        messageColor = TextColor.ANSI.RED;
                        message = String.format("%-35s", properties.getProperty("challenge.messages.started"));
                    } else {
                        try {
                            client.startChallenge();
                            message = String.format("%-35s", "");
                        } catch (UnauthorizedOperationException e) {
                            messageColor = TextColor.ANSI.RED;
                            message = String.format("%-35s", properties.getProperty("challenge.messages.unauthorized"));
                        } catch (NotEnoughPlayersException e) {
                            messageColor = TextColor.ANSI.RED;
                            message = String.format("%-35s", properties.getProperty("challenge.messages.notenoughplayers"));
                        }
                    }
                } else if (input.equals("!quit")) {
                    client.quitChallenge();
                    return;
                } else if (input.startsWith("!place")) {
                    input = input.replace("!place", "").replace(" ", "");

                    int number = 0;
                    int row;
                    int column;

                    if (input.length() != 3) {
                        valid = false;
                    } else {
                        try {
                            number = Integer.parseInt("" + input.charAt(0));
                        } catch (NumberFormatException e) {
                            valid = false;
                        }

                        row = input.charAt(1) - 'a';
                        column = input.charAt(2) - 'a';

                        if (valid) {
                            try {
                                client.placeNumber(row, column, number);

                                messageColor = TextColor.ANSI.GREEN;
                                message = String.format("%-35s", properties.getProperty("challenge.messages.correct"));
                            } catch (CellNotFoundException e) {
                                messageColor = TextColor.ANSI.RED;
                                message = String.format("%-35s", properties.getProperty("challenge.messages.cellnotfound"));
                            } catch (ChallengeStatusException e) {
                                if (client.getChallengeStatus() == ChallengeStatus.WAITING) {
                                    message = String.format("%-35s", properties.getProperty("challenge.messages.wait"));
                                } else {
                                    message = String.format("%-35s", properties.getProperty("challenge.messages.ended"));
                                }
                            } catch (FilledCellException e) {
                                messageColor = TextColor.ANSI.RED;
                                message = String.format("%-35s", properties.getProperty("challenge.messages.filled.1"));
                            } catch (FixedCellException e) {
                                messageColor = TextColor.ANSI.RED;
                                message = String.format("%-35s", properties.getProperty("challenge.messages.fixed"));
                            } catch (NumberAlreadyGuessedException e) {
                                messageColor = TextColor.ANSI.YELLOW;
                                message = String.format("%-35s", properties.getProperty("challenge.messages.filled.2"));
                            } catch (InvalidNumberException e) {
                                messageColor = TextColor.ANSI.RED;
                                message = String.format("%-35s", properties.getProperty("challenge.messages.wrong"));
                            }
                        }
                    }
                } else {
                    valid = false;
                }

                if (!valid) {
                    messageColor = TextColor.ANSI.RED;
                    message = String.format("%-35s", properties.getProperty("challenge.messages.invalid"));
                }

                command = new StringBuilder();
            } else if (keyStroke.getKeyType() == KeyType.Backspace) {
                if (command.length() == 0) {
                    continue;
                }

                command.deleteCharAt(command.length() - 1);

                textGraphics.putString(boardBottomRightEdge.withRelativeRow(-2).withRelativeColumn(2),
                        String.format("%-34s", command.toString()), SGR.BOLD);

                screen.setCursorPosition(boardBottomRightEdge.withRelativeRow(-2).withRelativeColumn(2 + command.length()));
            } else if (keyStroke.getKeyType() == KeyType.Character) {
                if (command.length() == 16) {
                    continue;
                }

                command.append(keyStroke.getCharacter());

                textGraphics.putString(boardBottomRightEdge.withRelativeRow(-2).withRelativeColumn(2),
                        command.toString(),
                        SGR.BOLD);

                screen.setCursorPosition(boardBottomRightEdge.withRelativeRow(-2).withRelativeColumn(2 + command.length()));
            }
        }

    }

    /**
     * Properly closes both connection and console.
     */
    private void close() throws Exception {
        screen.close();
        terminal.close();
        client.close();
    }

}
