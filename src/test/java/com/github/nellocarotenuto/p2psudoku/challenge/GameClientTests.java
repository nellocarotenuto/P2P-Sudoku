package com.github.nellocarotenuto.p2psudoku.challenge;

import com.github.nellocarotenuto.p2psudoku.sudoku.FilledCellException;
import com.github.nellocarotenuto.p2psudoku.sudoku.FixedCellException;
import com.github.nellocarotenuto.p2psudoku.sudoku.InvalidNumberException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GameClientTests {

    private GameClient client1;
    private GameClient client2;

    @BeforeEach
    public void createClients() throws Exception {
        client1 = new GameClientImpl(InetAddress.getByName("127.0.0.1"), 4001, 4001);
        client2 = new GameClientImpl(InetAddress.getByName("127.0.0.1"), 4001, 4002);
    }

    @AfterEach
    public void destroyClients() throws Exception {
        client2.close();
        client1.close();
    }

    @Test
    @DisplayName("Login test")
    public void testLogin() throws Exception {
        client1.login("Alice");

        assertEquals(client1.getNickname(), "Alice");
    }

    @Test
    @DisplayName("Login when already logged in")
    public void testLoginWhenAlreadyLoggedIn() throws Exception {
        client1.login("Alice");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            client1.login("Bob");
        });

        assertEquals(exception.getMessage(), "Already logged in.");
    }

    @Test
    @DisplayName("Login with invalid nickname")
    public void testLoginWithInvalidNickname() throws Exception {
        assertThrows(InvalidNicknameException.class, () -> {
            client1.login("Alice!");
        });
    }

    @Test
    @DisplayName("Login with taken nickname test")
    public void testLoginWithTakenNickname() throws Exception {
        client1.login("Alice");

        assertThrows(TakenNicknameException.class, () -> {
            client2.login("Alice");
        });
    }

    @Test
    @DisplayName("Logout test")
    public void testLogout() throws Exception {
        client1.login("Alice");
        client1.logout();

        Exception exception = assertThrows(RuntimeException.class, () -> {
            client1.getNickname();
        });

        assertEquals(exception.getMessage(), "Player not logged in.");
    }

    @Test
    @DisplayName("List players test")
    public void testListPlayers() throws Exception {
        client1.login("Alice");
        client2.login("Bob");

        assertEquals(client1.listPlayers().size(), 2);
        assertEquals(client2.listPlayers().size(), 2);
    }

    @Test
    @DisplayName("Create challenge when not logged in test")
    public void createChallengeWhenNotLoggedIn() throws Exception {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            client1.createChallenge("Challenge 1", 7, true);
        });

        assertEquals(exception.getMessage(), "Unable to create a new challenge if not logged in.");
    }

    @Test
    @DisplayName("Create challenge with invalid name test")
    public void testCreateChallengeWithInvalidName() throws Exception {
        client1.login("Alice");
        Exception exception = assertThrows(InvalidChallengeNameException.class, () -> {
            client1.createChallenge("Challenge 1!", 7, true);
        });

        assertEquals(exception.getMessage(), "A game name must be at least 3 characters long and only contain " +
                "letters, numbers, dashes, dots and underscores.");
    }

    @Test
    @DisplayName("Create listed challenge test")
    public void testCreateListedChallenge() throws Exception {
        client1.login("Alice");
        client1.createChallenge("Challenge 1", 7, true);

        assertEquals(client1.listChallenges().size(), 1);
    }

    @Test
    @DisplayName("Create unlisted challenge test")
    public void testCreateUnlistedChallenge() throws Exception {
        client1.login("Alice");
        client1.createChallenge("Challenge 1", 7, false);

        assertEquals(client1.listChallenges().size(), 0);
    }

    @Test
    @DisplayName("Create challenge when already participating to another one test")
    public void testCreateChallengeWhenAlreadyParticipatingToAnother() throws Exception {
        client1.login("Alice");
        client1.createChallenge("Challenge 1", 7, true);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            client1.createChallenge("Challenge 2", 42, true);
        });

        assertEquals(exception.getMessage(), "Unable to create a new challenge when already participating to " +
                "another one.");
    }

    @Test
    @DisplayName("Create challenge when another with the same name already exists test")
    public void testCreateChallengeWhenAnotherWithTheSameNameExists() throws Exception {
        client1.login("Alice");
        client2.login("Bob");

        client1.createChallenge("Challenge 1", 7, false);

        assertThrows(ChallengeAlreadyExistsException.class, () -> {
            client2.createChallenge("Challenge 1", 42, false);
        });
    }

    @Test
    @DisplayName("Join challenge test")
    public void testJoinChallenge() throws Exception {
        client1.login("Alice");
        client2.login("Bob");

        client1.createChallenge("Challenge 1", 7, false);
        client2.joinChallenge("Challenge 1");

        assertEquals(client2.getChallengeName(), "Challenge 1");
    }

    @Test
    @DisplayName("Join challenge when not logged in test")
    public void testJoinChallengeWhenNotLoggedIn() throws Exception {
        client1.login("Alice");
        client1.createChallenge("Challenge 1", 7, true);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            client2.joinChallenge("Challenge 1");
        });

        assertEquals(exception.getMessage(), "Unable to join a challenge if not logged in.");
    }

    @Test
    @DisplayName("Join challenge when already participating to another one test")
    public void testJoinChallengeWhenAlreadyParticipatingToAnother() throws Exception {
        client1.login("Alice");
        client2.login("Bob");

        client1.createChallenge("Challenge 1", 7, false);
        client2.createChallenge("Challenge 2", 42, false);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            client2.joinChallenge("Challenge 1");
        });

        assertEquals(exception.getMessage(), "Unable to join challenge while already playing another one.");
    }

    @Test
    @DisplayName("Join not existing challenge test")
    public void testJoinNotExistingChallenge() throws Exception {
        client1.login("Alice");

        assertThrows(ChallengeNotFoundException.class, () -> {
            client1.joinChallenge("Challenge 1");
        });
    }

    @Test
    @DisplayName("Quit challenge test")
    public void testQuitChallenge() throws Exception {
        client1.login("Alice");
        client2.login("Bob");

        client1.createChallenge("Challenge 1", 7, false);

        client2.joinChallenge("Challenge 1");
        client2.quitChallenge();

        Exception exception = assertThrows(RuntimeException.class, () -> {
            client2.getChallengeName();
        });

        assertEquals(exception.getMessage(), "Unable to get the name of the current challenge while not " +
                "participating to any.");
    }

    @Test
    @DisplayName("Quit challenge when alone and owning it test")
    public void testQuitChallengeWhenAloneAndOwningIt() throws Exception {
        client1.login("Alice");
        client2.login("Bob");

        client1.createChallenge("Challenge 1", 7, false);
        client1.quitChallenge();

        assertThrows(ChallengeNotFoundException.class, () -> {
            client2.joinChallenge("Challenge 1");
        });
    }

    @Test
    @DisplayName("Quit challenge when not alone and owning it test")
    public void testQuitChallengeWhenNotAloneAndOwningIt() throws Exception {
        client1.login("Alice");
        client2.login("Bob");

        client1.createChallenge("Challenge 1", 7, false);

        client2.joinChallenge("Challenge 1");

        client1.quitChallenge();

        assertEquals(client2.getChallengeOwnerNickname(), "Bob");
    }

    @Test
    @DisplayName("Start challenge test")
    public void testStartChallenge() throws Exception {
        client1.login("Alice");
        client2.login("Bob");

        client1.createChallenge("Challenge 1", 7, false);
        client2.joinChallenge("Challenge 1");

        client1.startChallenge();
        assertEquals(client2.getChallengeStatus(), ChallengeStatus.PLAYING);
    }

    @Test
    @DisplayName("Start challenge when not owning it test")
    public void testStartChallengeWhenNotOwningIt() throws Exception {
        client1.login("Alice");
        client2.login("Bob");

        client1.createChallenge("Challenge 1", 7, false);
        client2.joinChallenge("Challenge 1");

        assertThrows(UnauthorizedOperationException.class, () -> {
            client2.startChallenge();
        });
    }

    @Test
    @DisplayName("Start challenge when already started test")
    public void testStartChallengeWhenAlreadyStarted() throws Exception {
        client1.login("Alice");
        client2.login("Bob");

        client1.createChallenge("Challenge 1", 7, false);
        client2.joinChallenge("Challenge 1");

        client1.startChallenge();

        assertThrows(ChallengeStatusException.class, () -> {
            client1.startChallenge();
        });
    }

    @Test
    @DisplayName("Start challenge with not enough players test")
    public void testStartChallengeWithNotenoughPlayers() throws Exception {
        client1.login("Alice");

        client1.createChallenge("Challenge 1", 7, false);

        assertThrows(NotEnoughPlayersException.class, () -> {
            client1.startChallenge();
        });
    }

    @Test
    @DisplayName("Place number test")
    public void testPlaceNumber() throws Exception {
        client1.login("Alice");
        client2.login("Bob");

        client1.createChallenge("Challenge 1", 7, false);
        client2.joinChallenge("Challenge 1");

        client1.startChallenge();

        client1.placeNumber(7, 6, 4);

        assertEquals(client1.getChallengeBoard()[7][6], 4);
        assertEquals(client2.getChallengeBoard()[7][6], 0);

        assertEquals(client1.getChallengeScore(), Challenge.CORRECT_NUMBER_SCORE);
    }

    @Test
    @DisplayName("Place number already guessed test")
    public void testPlaceNumberAlreadyGuessed() throws Exception {
        client1.login("Alice");
        client2.login("Bob");

        client1.createChallenge("Challenge 1", 7, false);
        client2.joinChallenge("Challenge 1");

        client1.startChallenge();

        client1.placeNumber(7, 6, 4);

        assertThrows(NumberAlreadyGuessedException.class, () -> {
            client2.placeNumber(7, 6, 4);
        });

        assertEquals(client2.getChallengeScore(), 0);
    }

    @Test
    @DisplayName("Place number in filled cell test")
    public void testPlaceInFilledCell() throws Exception {
        client1.login("Alice");
        client2.login("Bob");

        client1.createChallenge("Challenge 1", 7, false);
        client2.joinChallenge("Challenge 1");

        client1.startChallenge();

        client1.placeNumber(7, 6, 4);

        assertThrows(FilledCellException.class, () -> {
            client1.placeNumber(7, 6, 5);
        });

        assertEquals(client1.getChallengeScore(), Challenge.CORRECT_NUMBER_SCORE);
    }

    @Test
    @DisplayName("Place number in a fixed cell test")
    public void testPlaceNumberInFixedcell() throws Exception {
        client1.login("Alice");
        client2.login("Bob");

        client1.createChallenge("Challenge 1", 7, false);
        client2.joinChallenge("Challenge 1");

        client1.startChallenge();

        assertThrows(FixedCellException.class, () -> {
            client1.placeNumber(7, 7, 8);
        });

        assertEquals(client1.getChallengeScore(), 0);
    }

    @Test
    @DisplayName("Place number wrong guess test")
    public void testPlaceNumberWrongGuess() throws Exception {
        client1.login("Alice");
        client2.login("Bob");

        client1.createChallenge("Challenge 1", 7, false);
        client2.joinChallenge("Challenge 1");

        client1.startChallenge();

        assertThrows(InvalidNumberException.class, () -> {
            client1.placeNumber(7, 6, 7);
        });

        assertEquals(client1.getChallengeScore(), Challenge.WRONG_NUMBER_SCORE);
    }

}
