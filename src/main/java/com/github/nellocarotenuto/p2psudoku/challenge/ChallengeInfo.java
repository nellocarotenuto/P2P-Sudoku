package com.github.nellocarotenuto.p2psudoku.challenge;

import java.io.Serializable;
import java.util.Objects;

/**
 * Models a simpler object to represent public info of a challenge.
 */
public class ChallengeInfo implements Serializable {

    private static final long serialVersionUID = 8175570788346488662L;

    private String name;
    private String owner;
    private ChallengeStatus status;
    private int players;

    /**
     * Creates a new object representing the info of a challenge.
     *
     * @param challenge the challenge to represent
     */
    public ChallengeInfo(Challenge challenge) {
        name = challenge.getName();
        owner = challenge.getOwner().getNickname();
        status = challenge.getStatus();
        players = challenge.getGames().keySet().size();
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public ChallengeStatus getStatus() {
        return status;
    }

    public void setStatus(ChallengeStatus status) {
        this.status = status;
    }

    public int getPlayers() {
        return players;
    }

    public void setPlayers(int players) {
        this.players = players;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        ChallengeInfo info = (ChallengeInfo) object;
        return name.equals(info.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "ChallengeInfo{" +
                "\n\tname='" + name + "'," +
                "\n\towner='" + owner + "'," +
                "\n\tstatus='" + status + "'," +
                "\n\tplayers='" + players + "'" +
                "\n}";
    }
}
