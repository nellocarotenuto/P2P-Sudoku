package com.github.nellocarotenuto.p2psudoku.challenge;

import java.io.Serializable;
import java.util.Objects;

/**
 * Models a simpler object to represent public info of a challenge.
 */
public class Info implements Serializable {

    private static final long serialVersionUID = 8175570788346488662L;

    private String name;
    private String owner;
    private Challenge.Status status;
    private int players;

    /**
     * Creates a new object representing the info of a challenge.
     *
     * @param name the name of the challenge
     * @param owner the name of the owner of the challenge
     * @param status the status of the challenge
     */
    public Info(String name, String owner, Challenge.Status status, int players) {
        this.name = name;
        this.owner = owner;
        this.status = status;
        this.players = players;
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

    public Challenge.Status getStatus() {
        return status;
    }

    public void setStatus(Challenge.Status status) {
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

        Info info = (Info) object;
        return name.equals(info.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Info{" +
                "\n\tname='" + name + "'," +
                "\n\towner='" + owner + "'," +
                "\n\tstatus='" + status + "'," +
                "\n\tplayers='" + players + "'" +
                "\n}";
    }
}
