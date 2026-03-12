package fr.emse.warehouse;

public class RobotMessage {
    private final MessageType type;
    private final int senderRobotId;
    private final Integer receiverRobotId;
    private final Integer palletId;
    private final double score;
    private final String payload;

    public RobotMessage(
        MessageType type,
        int senderRobotId,
        Integer receiverRobotId,
        Integer palletId,
        double score,
        String payload
    ) {
        this.type = type;
        this.senderRobotId = senderRobotId;
        this.receiverRobotId = receiverRobotId;
        this.palletId = palletId;
        this.score = score;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public int getSenderRobotId() {
        return senderRobotId;
    }

    public Integer getReceiverRobotId() {
        return receiverRobotId;
    }

    public Integer getPalletId() {
        return palletId;
    }

    public double getScore() {
        return score;
    }

    public String getPayload() {
        return payload;
    }
}
