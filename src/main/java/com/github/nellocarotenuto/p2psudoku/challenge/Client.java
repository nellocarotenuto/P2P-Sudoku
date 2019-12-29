package com.github.nellocarotenuto.p2psudoku.challenge;

import com.github.nellocarotenuto.p2psudoku.utils.ElementAlreadyExistsException;
import com.github.nellocarotenuto.p2psudoku.utils.ElementNotFoundException;
import com.github.nellocarotenuto.p2psudoku.utils.FailedOperationException;
import com.github.nellocarotenuto.p2psudoku.utils.PeerDHTUtils;

import java.net.InetAddress;
import java.util.*;

import net.tomp2p.dht.*;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.Number640;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;
import net.tomp2p.utils.Pair;

import org.javatuples.Triplet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exposes game's public API and implements its logic.
 */
public class Client {

    private enum Notification {
        CHALLENGES_LIST_UPDATED,
        CHALLENGE_UPDATED
    }

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private static final int MAX_SYNC_ATTEMPTS = 10;

    public static int DEFAULT_PORT = 4001;

    private Peer peer;
    private PeerDHT dht;

    private Random random;

    private Player player;
    private List<ChallengeInfo> challenges;
    private Challenge challenge;

    public Client(InetAddress masterAddress, int masterPort, int localPort) throws Exception {
        // Define the random number generator
        random = new Random();

        // Define the peer and the DHT
        peer = new PeerBuilder(new Number160(random)).ports(localPort).start();
        dht = new PeerBuilderDHT(peer).start();

        // Bootstrap to master peer
        if (!InetAddress.getLocalHost().equals(masterAddress) || masterPort != localPort) {
            FutureBootstrap bootstrap = peer.bootstrap()
                    .inetAddress(masterAddress)
                    .ports(masterPort)
                    .start();
            bootstrap.awaitUninterruptibly();

            if (bootstrap.isSuccess()) {
                peer.discover().peerAddress(bootstrap.bootstrapTo().iterator().next()).start().awaitUninterruptibly();
            } else {
                throw new RuntimeException("Unable to bootstrap to " + masterAddress.getHostAddress() + ":" + masterPort + ".");
            }
        }

        // Initialize the list of players if it doesn't already exist in the DHT
        try {
            PeerDHTUtils.get(dht, Number160.ZERO);
        } catch (ElementNotFoundException e) {
            List<Player> players = new ArrayList<>();
            PeerDHTUtils.create(dht, Number160.ZERO, new Data(players));
        }

        // Initialize the list of public games if it doesn't already exist in the DHT
        try {
            PeerDHTUtils.get(dht, Number160.ONE);
        } catch (ElementNotFoundException e) {
            List<ChallengeInfo> challenges = new ArrayList<>();
            PeerDHTUtils.create(dht, Number160.ONE, new Data(challenges));
        }

        // Define a listener to handle notifications
        peer.objectDataReply(new ObjectDataReply() {

            @Override
            public Object reply(PeerAddress sender, Object request) throws Exception {
                if (!(request instanceof Notification)) {
                    throw new RuntimeException("Unable to handle the message received.");
                }

                if (request == Notification.CHALLENGE_UPDATED) {
                    syncChallenge();
                } else if (request == Notification.CHALLENGES_LIST_UPDATED) {
                    syncChallengesList();
                }

                return request;
            }
        });
    }

    /**
     * Allows to log into the system specifying a unique nickname used for the identification.
     *
     * @param nickname the String representing the nickname of the user logging into the system
     *
     * @throws InvalidNicknameException if the nickname passed as parameter doesn't match the predefined pattern
     * @throws TakenNicknameException if the nickname picked has already been chosen by another user
     */
    @SuppressWarnings("unchecked")
    public void login(String nickname) throws Exception {
        if (player != null) {
            throw new RuntimeException("Already logged in.");
        }

        if (!nickname.matches(Challenge.NICKNAME_FORMAT)) {
            throw new InvalidNicknameException("A nickname must be at least 3 characters long and only contain " +
                                               "letters, numbers, dashes, dots or underscores.");
        }

        Player player = new Player(nickname, peer.peerAddress());

        for (int attempt = 0; attempt < MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                Pair<Number640, Data> entry = PeerDHTUtils.get(dht, Number160.ZERO);
                Number640 key = entry.element0();
                List<Player> players = (List<Player>) entry.element1().object();

                if (players.contains(player)) {
                    throw new TakenNicknameException("Nickname \"" + nickname + "\" has already been taken.");
                }

                players.add(player);

                PeerDHTUtils.update(dht, new Pair<>(key, new Data(players)));
                this.player = player;

                logger.debug("Player " + player.getNickname() + " logged in");

                break;
            } catch (FailedOperationException e) {
                logger.debug("Login attempt " + (attempt + 1) + " failed");

                if (attempt == MAX_SYNC_ATTEMPTS - 1) {
                    throw new RuntimeException("Unable to update the list of currently logged in users.");
                }

                Thread.sleep(random.nextInt(500));
            }
        }

        syncChallengesList();
    }

    /**
     * Allows a currently logged in user to log out of the system.
     */
    @SuppressWarnings("unchecked")
    public void logout() throws Exception {
        if (player == null) {
            return;
        }

        if (challenge != null) {
            quitChallenge();
        }

        for (int attempt = 0; attempt < MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                Pair<Number640, Data> entry = PeerDHTUtils.get(dht, Number160.ZERO);
                List<Player> players = (List<Player>) entry.element1().object();

                players.remove(player);

                PeerDHTUtils.update(dht, new Pair<>(entry.element0(), new Data(players)));

                logger.debug("Player " + player.getNickname() + " logged out");

                this.player = null;
                break;
            } catch (FailedOperationException e) {
                logger.debug("Logout attempt " + (attempt + 1) + " failed");

                if (attempt == MAX_SYNC_ATTEMPTS - 1) {
                    throw new RuntimeException("Unable to update the list of currently logged in users.");
                }

                Thread.sleep(random.nextInt(500));
            }
        }
    }

    /**
     * Retrieves the list of players currently logged into the system.
     *
     * @return the current list of logged in players
     */
    @SuppressWarnings("unchecked")
    public List<Player> listPlayers() throws Exception {
        for (int attempt = 0; attempt < MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                return (List<Player>) PeerDHTUtils.get(dht, Number160.ZERO).element1().object();
            } catch (FailedOperationException e) {
                logger.debug("Listing players attemp " + (attempt + 1) + " failed");

                Thread.sleep(random.nextInt(500));
            }
        }

        throw new RuntimeException("Unable to fetch the list of currently logged in users.");
    }

    /**
     * Gets the nickname of the player currently logged in.
     *
     * @return the nickname of the player currently logged in
     */
    public String getNickname() {
        if (player == null) {
            throw new RuntimeException("Player not logged in.");
        }

        return player.getNickname();
    }

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
    public void createChallenge(String name, int seed, boolean listed) throws Exception {
        if (player == null) {
            throw new RuntimeException("Unable to create a new challenge if not logged in.");
        }

        if (challenge != null) {
            throw new RuntimeException("Unable to create a new challenge when already participating to another one.");
        }

        Challenge challenge = new Challenge(player, name, seed, listed);
        challenge.addPlayer(player);

        for (int attempt = 0; attempt < MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                PeerDHTUtils.create(dht, Number160.createHash(name), new Data(challenge));
                this.challenge = challenge;

                logger.debug("Player " + player.getNickname() + " created challenge " + challenge.getName());

                break;
            } catch (ElementAlreadyExistsException e) {
                logger.debug("Player " + player.getNickname() + " attempted to create " + challenge.getName() +
                             "but one with the same name already exists");

                throw new ChallengeAlreadyExistsException("Challenge " + challenge.getName() + " already exists");
            } catch (FailedOperationException e) {
                logger.debug("Challenge creation attempt " + (attempt + 1) + " failed");

                if (attempt == MAX_SYNC_ATTEMPTS - 1) {
                    throw new RuntimeException("Unable to create the challenge.");
                }

                Thread.sleep(random.nextInt(500));
            }
        }

        if (listed) {
            addChallengeToList(challenge);
        }
    }

    /**
     * Gets the list of public challenges in the system.
     *
     * @return the list of public challenges available in the system
     */
    public List<ChallengeInfo> listChallenges() {
        return challenges;
    }

    /**
     * Allows the player to join a game.
     *
     * @param name the name of the challenge to join
     *
     * @throws JoinException if no game with the specified name exists
     */
    public void joinChallenge(String name) throws Exception {
        if (player == null) {
            throw new RuntimeException("Unable to join a challenge if not logged in.");
        }

        if (challenge != null) {
            if (!challenge.getName().equals(name)) {
                throw new RuntimeException("Unable to join challenge while already playing another one.");
            } else {
                return;
            }
        }

        Challenge challenge = null;

        for (int attempt = 0; attempt < MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                Pair<Number640, Data> entry = PeerDHTUtils.get(dht, Number160.createHash(name));
                Number640 key = entry.element0();
                challenge = (Challenge) entry.element1().object();

                challenge.addPlayer(player);

                PeerDHTUtils.update(dht, new Pair<>(key, new Data(challenge)));
                this.challenge = challenge;

                logger.debug("Player " + player.getNickname() + " joined challenge " + challenge.getName());

                notifyUpdate(Notification.CHALLENGE_UPDATED);
                break;
            } catch (ElementNotFoundException e) {
                throw new JoinException("Challenge " + name + " doesn't exist.");
            } catch (FailedOperationException e) {
                logger.debug("Join attempt " + (attempt + 1) + " failed");

                if (attempt == MAX_SYNC_ATTEMPTS - 1) {
                    throw new RuntimeException("Unable to update the challenge.");
                }

                Thread.sleep(random.nextInt(500));
            }
        }

        if (challenge.isListed()) {
            updateChallengeInList(challenge);
        }
    }

    /**
     * Allows the player to quit the current challenge.
     *
     * The challenge is deleted if there was only one player participating.
     * The ownership is handed over to another player participating to the challenge if the owner quits.
     */
    public void quitChallenge() throws Exception {
        if (challenge == null) {
            return;
        }

        boolean challengeDeleted = false;

        Challenge challenge = null;

        for (int attempt = 0; attempt < MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                Pair<Number640, Data> entry = PeerDHTUtils.get(dht, Number160.createHash(this.challenge.getName()));
                Number640 key = entry.element0();

                challenge = (Challenge) entry.element1().object();
                challenge.removePlayer(player);

                if (challenge.getGames().size() == 0) {
                    PeerDHTUtils.remove(dht, Number160.createHash(challenge.getName()));
                    challengeDeleted = true;
                } else {
                    if (challenge.getOwner().equals(player)) {
                        HashMap<Player, Triplet<Integer[][], Integer, Boolean>> games = challenge.getGames();
                        List<Player> players = new ArrayList<>(games.keySet());
                        Player newOwner = players.get(random.nextInt(players.size()));
                        challenge.setOwner(newOwner);

                        logger.debug("Challenge " + challenge.getName() + "'s owner changed to " + newOwner.getNickname());
                    }

                    PeerDHTUtils.update(dht, new Pair<>(key, new Data(challenge)));
                    notifyUpdate(Notification.CHALLENGE_UPDATED);

                    logger.debug("Player " + player.getNickname() + " quit challenge " + challenge.getName());
                }

                this.challenge = null;
                break;
            } catch (FailedOperationException e) {
                logger.debug("Quit attempt " + (attempt + 1) + " failed");

                if (attempt == MAX_SYNC_ATTEMPTS - 1) {
                    throw new RuntimeException("Unable to update the challenge.");
                }

                Thread.sleep(random.nextInt(500));
            }
        }

        if (challenge.isListed()) {
            if (challengeDeleted) {
                removeChallengeFromList(challenge);
            } else {
                updateChallengeInList(challenge);
            }
        }
    }

    /**
     * Allows the user to start the challenge.
     *
     * @throws UnauthorizedOperationException if the player is not the owner of the challenge
     */
    public void startChallenge() throws Exception {
        if (player == null) {
            throw new RuntimeException("Unable to start a challenge if not logged in.");
        }

        if (challenge == null) {
            throw new RuntimeException("Unable to start a challenge if not participating to one.");
        }

        Challenge challenge = null;

        for (int attempt = 0; attempt < MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                Pair<Number640, Data> entry = PeerDHTUtils.get(dht, Number160.createHash(this.challenge.getName()));
                Number640 key = entry.element0();
                challenge = (Challenge) entry.element1().object();

                challenge.start(player);

                PeerDHTUtils.update(dht, new Pair<>(key, new Data(challenge)));
                this.challenge = challenge;

                notifyUpdate(Notification.CHALLENGE_UPDATED);

                logger.debug("Player " + player.getNickname() + " started the challenge " + challenge.getName());

                break;
            } catch (FailedOperationException e) {
                logger.debug("Challenge starting attempt " + (attempt + 1) + " failed");

                if (attempt == MAX_SYNC_ATTEMPTS - 1) {
                    throw new RuntimeException("Unable to update the challenge.");
                }

                Thread.sleep(random.nextInt(500));
            }
        }

        if (challenge.isListed()) {
            updateChallengeInList(challenge);
        }
    }

    /**
     * Lets the user place a number in the board.
     *
     * @param row the row index (starting at 0) of the cell where to insert the number
     * @param column the column index (starting at 0) of the cell where to insert the number
     * @param number the number to put into the cell
     *
     * @throws ChallengeStatusException if the challenge has not started or already ended
     */
    public void placeNumber(int row, int column, int number) throws Exception {
        if (challenge == null) {
            throw new RuntimeException("Unable to place a number if not participating to a challenge.");
        }

        for (int attempt = 0; attempt < MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                Pair<Number640, Data> entry = PeerDHTUtils.get(dht, Number160.createHash(this.challenge.getName()));
                Number640 key = entry.element0();
                Challenge challenge = (Challenge) entry.element1().object();

                try {
                    challenge.placeNumber(player, row, column, number);
                } finally {
                    PeerDHTUtils.update(dht, new Pair<>(key, new Data(challenge)));
                    this.challenge = challenge;

                    notifyUpdate(Notification.CHALLENGE_UPDATED);

                    logger.debug("Player " + player.getNickname() + " placed a number in challenge " + challenge.getName());
                }

                return;
            } catch (FailedOperationException e) {
                logger.debug("Number placement attempt " + (attempt + 1) + " failed");

                if (attempt == MAX_SYNC_ATTEMPTS - 1) {
                    throw new RuntimeException("Unable to update the challenge.");
                }

                Thread.sleep(random.nextInt(500));
            }
        }

        throw new RuntimeException("Unable to update the challenge.");
    }

    /**
     * Gets the name of the current challenge.
     *
     * @return the name of the current challenge
     */
    public String getChallengeName() {
        if (challenge == null) {
            throw new RuntimeException("Unable to get the name of the current challenge while not participating to any.");
        }

        return challenge.getName();
    }

    /**
     * Gets the board of the user for the current challenge.
     *
     * @return the integer matrix representing user's board for the current matrix
     */
    public Integer[][] getChallengeBoard() {
        if (challenge == null) {
            throw new RuntimeException("Unable to get the current board while not participating to any challenge.");
        }

        return challenge.getBoard(player);
    }

    /**
     * Gets the score of the user for the current challenge.
     *
     * @return the integer score of the user for the current challenge
     */
    public int getChallengeScore() {
        if (challenge == null) {
            throw new RuntimeException("Unable to get the current score while not participating to any challenge.");
        }

        return challenge.getGames().get(player).getValue1();
    }

    /**
     * Returns the scores of the players participating to the challenge.
     *
     * @return the list of players participating to the challenge sorted by their scores
     */
    public List<Pair<String, Integer>> getChallengeScores() {
        if (challenge == null) {
            throw new RuntimeException("Unable to get other players' scores while not participating to any challenge.");
        }

        List<Pair<String, Integer>> scores = new ArrayList<>();

        for (Player player : challenge.getGames().keySet()) {
            Triplet<Integer[][], Integer, Boolean> game = challenge.getGames().get(player);

            scores.add(new Pair<>(player.getNickname(), game.getValue1()));
        }

        scores.sort(new Comparator<Pair<String, Integer>>() {
            @Override
            public int compare(Pair<String, Integer> pair1, Pair<String, Integer> pair2) {
                return pair2.element1().compareTo(pair1.element1());
            }
        });

        return scores;
    }

    /**
     * Tells whether the player currently logged in is the owner of the challenge to which he's participating.
     *
     * @return true if the player logged in is the owner of the current challenge, false otherwise
     */
    public boolean isChallengeOwner() {
        if (player == null || challenge == null) {
            throw new RuntimeException("The player has to be logged in and participating to a challenge");
        }

        return player.equals(challenge.getOwner());
    }

    /**
     * Gets the nickname of the owner of the current challenge.
     *
     * @return the nickname of the owner of the current challenge
     */
    public String getChallengeOwnerNickname() {
        if (challenge == null) {
            throw new RuntimeException("Unable to get owner's nickname if not participating to any challenge.");
        }

        return challenge.getOwner().getNickname();
    }

    /**
     * Returns the status of the challenge to which the player is participating.
     *
     * @return the status of the current challenge
     */
    public ChallengeStatus getChallengeStatus() {
        if (challenge == null) {
            throw new RuntimeException("Unable to get the game status if not participating to any challenge.");
        }

        return challenge.getStatus();
    }

    /**
     * Properly leaves the network by logging out and announcing the shutdown the others.
     */
    public void close() throws Exception {
        if (player != null) {
            logout();
        }

        peer.shutdown();
    }

    /**
     * Adds a challenge to the list of public ones.
     *
     * @param challenge the challenge to add to the list
     */
    @SuppressWarnings("unchecked")
    private void addChallengeToList(Challenge challenge) throws Exception {
        for (int attempt = 0; attempt < MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                Pair<Number640, Data> entry = PeerDHTUtils.get(dht, Number160.ONE);
                Number640 key = entry.element0();
                List<ChallengeInfo> challenges = (List<ChallengeInfo>) entry.element1().object();

                challenges.add(challenge.getInfo());

                PeerDHTUtils.update(dht, new Pair<>(key, new Data(challenges)));
                this.challenges = challenges;

                notifyUpdate(Notification.CHALLENGES_LIST_UPDATED);

                logger.debug("Challenge " + challenge.getName() + " added to the list");

                break;
            } catch (FailedOperationException e) {
                logger.debug("Challenges list update attempt " + (attempt + 1) + " failed");

                if (attempt == MAX_SYNC_ATTEMPTS - 1) {
                    throw new RuntimeException("Unable to update the list of public challenges.");
                }

                Thread.sleep(random.nextInt(500));
            }
        }
    }

    /**
     * Updates the info of a challenge in the list of public ones.
     *
     * @param challenge the challenge whose info should be updated
     */
    @SuppressWarnings("unchecked")
    private void updateChallengeInList(Challenge challenge) throws Exception {
        for (int attempt = 0; attempt < MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                Pair<Number640, Data> entry = PeerDHTUtils.get(dht, Number160.ONE);
                Number640 key = entry.element0();
                List<ChallengeInfo> challenges = (List<ChallengeInfo>) entry.element1().object();

                int index = challenges.indexOf(challenge.getInfo());
                challenges.set(index, challenge.getInfo());

                PeerDHTUtils.update(dht, new Pair<>(key, new Data(challenges)));
                this.challenges = challenges;

                notifyUpdate(Notification.CHALLENGES_LIST_UPDATED);

                logger.debug("Challenge " + challenge.getName() + " updated in the list");

                break;
            } catch (FailedOperationException e) {
                logger.debug("Challenges list update attempt " + (attempt + 1) + " failed");

                if (attempt == MAX_SYNC_ATTEMPTS - 1) {
                    throw new RuntimeException("Unable to update the list of public challenges.");
                }

                Thread.sleep(random.nextInt(500));
            }
        }
    }

    /**
     * Removes a challenge from the list of public ones.
     *
     * @param challenge the challenge to remove from the list
     */
    @SuppressWarnings("unchecked")
    private void removeChallengeFromList(Challenge challenge) throws Exception {
        for (int attempt = 0; attempt < MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                Pair<Number640, Data> entry = PeerDHTUtils.get(dht, Number160.ONE);
                Number640 key = entry.element0();
                List<ChallengeInfo> challenges = (List<ChallengeInfo>) entry.element1().object();

                challenges.remove(challenge.getInfo());

                PeerDHTUtils.update(dht, new Pair<>(key, new Data(challenges)));
                this.challenges = challenges;

                notifyUpdate(Notification.CHALLENGES_LIST_UPDATED);

                logger.debug("Challenge " + challenge.getName() + " updated in the list");

                break;
            } catch (FailedOperationException e) {
                logger.debug("Challenges list update attempt " + (attempt + 1) + " failed");

                if (attempt == MAX_SYNC_ATTEMPTS - 1) {
                    throw new RuntimeException("Unable to update the list of public challenges.");
                }

                Thread.sleep(random.nextInt(500));
            }
        }
    }

    /**
     * Allows to send notifications to the players of the network.
     *
     * @param notification the type of notification to send
     */
    private void notifyUpdate(Notification notification) throws Exception {
        List<Player> players;
        FutureDirect[] directs;

        if (notification == Notification.CHALLENGE_UPDATED) {
            players = new ArrayList<>(challenge.getGames().keySet());
        } else if (notification == Notification.CHALLENGES_LIST_UPDATED) {
            players = listPlayers();
        } else {
            throw new RuntimeException("Unable to send message due to its type being unknown.");
        }

        players.remove(player);
        directs = new FutureDirect[players.size()];

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);

            directs[i] = peer.sendDirect(player.getAddress())
                    .object(notification)
                    .start();
        }

        for (FutureDirect direct : directs) {
            while (!direct.isCompleted());
        }
    }

    /**
     * Updates the current challenge to the latest version available in the DHT.
     */
    private void syncChallenge() throws Exception {
        if (challenge == null) {
            return;
        }

        for (int attempt = 0; attempt < MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                Pair<Number640, Data> entry = PeerDHTUtils.get(dht, Number160.createHash(challenge.getName()));
                challenge = (Challenge) entry.element1().object();

                logger.debug("Challenge synchronized");

                return;
            } catch (FailedOperationException e) {
                logger.debug("Challenge sync attempt " + (attempt + 1) + " failed");

                if (attempt == MAX_SYNC_ATTEMPTS - 1) {
                    throw new RuntimeException("Unable to fetch the current challenge.");
                }

                Thread.sleep(random.nextInt(500));
            }
        }
    }

    /**
     * Updates the current challenge to the latest version available in the DHT.
     */
    @SuppressWarnings("unchecked")
    private void syncChallengesList() throws Exception {
        for (int attempt = 0; attempt < MAX_SYNC_ATTEMPTS; attempt++) {
            try {
                Pair<Number640, Data> entry = PeerDHTUtils.get(dht, Number160.ONE);
                challenges = (List<ChallengeInfo>) entry.element1().object();

                logger.debug("Challenges list synchronized");

                return;
            } catch (FailedOperationException e) {
                logger.debug("Challenges list sync attempt " + (attempt + 1) + " failed");

                if (attempt == MAX_SYNC_ATTEMPTS - 1) {
                    throw new RuntimeException("Unable to fetch the list of public challenges.");
                }

                Thread.sleep(random.nextInt(500));
            }
        }
    }

}
