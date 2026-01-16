package org.markeb.game.manager;

import org.markeb.game.actor.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    private final Map<String, Player> playerMap = new ConcurrentHashMap<>();

    private static final PlayerManager INSTANCE = new PlayerManager();

    private PlayerManager() {

    }

    public static PlayerManager getInstance() {
        return INSTANCE;
    }

    public void addPlayer(Player player) {
        playerMap.put(player.getPlayerId(), player);
    }

    public Player getPlayer(String id) {
        return playerMap.get(id);
    }

    public void removePlayer(String id) {
        playerMap.remove(id);
    }

}
