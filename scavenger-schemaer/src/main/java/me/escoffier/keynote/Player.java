package me.escoffier.keynote;


import io.vertx.core.shareddata.Shareable;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@ProtoDoc("@Indexed")
@ProtoMessage(name = "Player")
public class Player implements Shareable {

    private int score;
    private String playerId;
    private String playerName;
    private String email;

    private List<Achievement> achievements = new CopyOnWriteArrayList<>();

    // Required for proto schema builder
    public Player() {
        this.playerId = UUID.randomUUID().toString();
        this.playerName = UserNameGenerator.generate();
        this.score = 0;
    }

    public Player(String uuid, String username, int score) {
        this.playerId = uuid;
        this.playerName = username;
        this.score = score;
    }

    public Player(String uuid, String username) {
        this.playerId = uuid;
        this.playerName = username;
    }

    @ProtoDoc("@IndexedField")
    @ProtoField(number = 10, required = true)
    public int getScore() {
        return score;
    }

    @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
    @ProtoField(number = 20, required = true)
    public String getPlayerId() {
        return playerId;
    }

    @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
    @ProtoField(number = 30, required = true)
    public String getPlayerName() {
        return playerName;
    }

    @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
    @ProtoField(number = 40, collectionImplementation = CopyOnWriteArrayList.class)
    public List<Achievement> getAchievements() {
        return achievements;
    }

    @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
    @ProtoField(number = 50, required = false)
    public String getEmail() {
        return email;
    }

    /**
     * Should only used by mappers, the score is computed using achievements.
     *
     * @param score the score
     * @return the current player.
     */
    public void setScore(int score) {
        this.score = score;
    }

    public void setPlayerId(String uuid) {
        this.playerId = uuid;
    }

    public void setPlayerName(String username) {
        this.playerName = username;
    }

    public void setAchievements(List<Achievement> achievements) {
        if (achievements == null) {
            this.achievements = new CopyOnWriteArrayList<>();
        } else {
            this.achievements = achievements;
        }
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<Achievement> achievements() {
        return new ArrayList<>(achievements);
    }

    public Player addAchievement(Achievement achievement) {
        achievements.add(achievement);
        score = this.score + achievement.point();
        return this;
    }

    /**
     * Adds an achievements.
     *
     * @param taskId        the achieved task
     * @param transactionId the transaction id
     * @param point         the number of point won by this achievement
     * @return {@code true} if the achievement has been added, {@code false} otherwise. In this latter case, it means that
     * the player has already completed this task and is a cheater!
     */
    public boolean achieved(String taskId, String transactionId, int point) {
        if (!alreadyAchieved(taskId)) {
            this.addAchievement(new Achievement(taskId, transactionId, point));
            return true;
        }
        return false;
    }

    private boolean alreadyAchieved(String taskId) {
        return achievements.stream().anyMatch(a -> a.getTaskId().equalsIgnoreCase(taskId));
    }

    public String name() {
        return playerName;
    }

    public String id() {
        return playerId;
    }

    public int score() {
        return score;
    }

}
