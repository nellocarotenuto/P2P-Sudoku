package com.github.nellocarotenuto.p2psudoku.challenge;

import org.javatuples.Pair;

import java.util.List;

/**
 * Defines the public API for the game.
 */
public interface GameClient {

    /**
     * Allows to log into the system specifying a unique nickname used for the identification.
     *
     * @param nickname the String representing the nickname of the user logging into the system
     *
     * @throws InvalidNicknameException if the nickname passed as parameter doesn't match the predefined pattern
     * @throws TakenNicknameException if the nickname picked has already been chosen by another user
     */
    public void login(String nickname) throws Exception;

    /**
     * Allows a currently logged in user to log out of the system.
     */
    public void logout() throws Exception;

    /**
     * Retrieves the list of players currently logged into the system.
     *
     * @return the current list of logged in players
     */
    public List<Player> listPlayers() throws Exception;

    /**
     * Gets the nickname of the player currently logged in.
     *
     * @return the nickname of the player currently logged in
     */
    public String getNickname();

    /**
     * Creates a new challenge and joins it automatically.
     *
     * @param name the name of the challenge
     * @param seed the seed of the board
     * @param listed true if the challenge is to be public, false otherwise
     *
     * @throws ChallengeAlreadyExistsException if a challenge with the same name already exists
     * @throws InvalidChallengeNameException if the name chosen doesn't match the pattern
     */
    public void createChallenge(String name, int seed, boolean listed) throws Exception;

    /**
     * Gets the list of public challenges in the system.
     *
     * @return the list of public challenges available in the system
     */
    public List<ChallengeInfo> listChallenges();

    /**
     * Allows the player to join a game.
     *
     * @param name the name of the challenge to join
     *
     * @throws ChallengeNotFoundException if no challenge with the specified name exists
     */
    public void joinChallenge(String name) throws Exception;

    /**
     * Allows the player to quit the current challenge.
     *
     * The challenge is deleted if there was only one player participating.
     * The ownership is handed over to another player participating to the challenge if the owner quits.
     */
    public void quitChallenge() throws Exception;

    /**
     * Allows the user to start the challenge.
     *
     * @throws UnauthorizedOperationException if the player is not the owner of the challenge
     */
    public void startChallenge() throws Exception;

    /**
     * Lets the user place a number in the board and updates his score accordingly.
     *
     * @param row the row index (starting at 0) of the cell where to insert the number
     * @param column the column index (starting at 0) of the cell where to insert the number
     * @param number the number to put into the cell
     *
     * @throws ChallengeStatusException if the challenge has not started or already ended
     */
    public void placeNumber(int row, int column, int number) throws Exception;

    /**
     * Gets the name of the current challenge.
     *
     * @return the name of the current challenge
     */
    public String getChallengeName();

    /**
     * Gets the board of the user for the current challenge.
     *
     * @return the integer matrix representing user's board for the current matrix
     */
    public Integer[][] getChallengeBoard();

    /**
     * Gets the score of the user for the current challenge.
     *
     * @return the integer score of the user for the current challenge
     */
    public int getChallengeScore();

    /**
     * Returns the scores of the players participating to the challenge.
     *
     * @return the list of players participating to the challenge sorted by their scores
     */
    public List<Pair<String, Integer>> getChallengeScores();


    /**
     * Tells whether the player currently logged in is the owner of the challenge to which he's participating.
     *
     * @return true if the player logged in is the owner of the current challenge, false otherwise
     */
    public boolean isChallengeOwner();

    /**
     * Gets the nickname of the owner of the current challenge.
     *
     * @return the nickname of the owner of the current challenge
     */
    public String getChallengeOwnerNickname();

    /**
     * Returns the status of the challenge to which the player is participating.
     *
     * @return the status of the current challenge
     */
    public ChallengeStatus getChallengeStatus();

    /**
     * Properly leaves the network by logging out and announcing the shutdown the others.
     */
    public void close() throws Exception;

}
