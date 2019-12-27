package com.github.nellocarotenuto.p2psudoku.challenge;

import net.tomp2p.peers.PeerAddress;

import java.io.Serializable;
import java.util.Objects;

/**
 * Models a player of the game.
 */
public class Player implements Serializable {

    private static final long serialVersionUID = 8751148151415174141L;

    private String nickname;
    private PeerAddress address;

    public Player(String nickname, PeerAddress address) {
        this.nickname = nickname;
        this.address = address;
    }

    public String getNickname() {
        return nickname;
    }

    public PeerAddress getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "Player{" +
                "\n\tnickname='" + nickname + "'," +
                "\n\taddress=" + address +
                "\n}";
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        Player player = (Player) object;
        return nickname.equals(player.nickname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nickname);
    }

}
