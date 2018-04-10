package me.escoffier.keynote;

import io.vertx.core.shareddata.Shareable;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents an achievement.
 */
@ProtoDoc("@Indexed")
@ProtoMessage(name = "Achievement")
public class Achievement implements Serializable, Shareable {

    private String taskId;

    private String transactionId;

    private int point;

    // Required for proto schema builder
    public Achievement() {
    }

    public Achievement(String taskId, String transactionId, int point) {
        this.taskId = Objects.requireNonNull(taskId);
        this.transactionId = Objects.requireNonNull(transactionId);
        if (point <= 0) {
            throw new IllegalArgumentException("Invalid number of point to create an achievement: " + point);
        }
        this.point = point;
    }

    public String task() {
        return taskId;
    }

    public String transaction() {
        return transactionId;
    }

    public int point() {
        return point;
    }

    @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
    @ProtoField(number = 10, required = true)
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
    @ProtoField(number = 20, required = true)
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    @ProtoDoc("@Field(index = Index.NO, store = Store.NO)")
    @ProtoField(number = 30, required = true)
    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point = point;
    }
}
