package org.markeb.game.actor;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import org.markeb.net.msg.IGameParser;
import org.markeb.net.register.GameActorContext;
import org.markeb.proto.notice.Forward.PushNotice;
import io.netty.channel.Channel;

public class Player implements GameActorContext {

    private final String playerId;
    private final Channel channel;
    private final IGameParser<Message> gameParser;

    public Player(String playerId, Channel channel, IGameParser<Message> gameParser) {
        this.playerId = playerId;
        this.channel = channel;
        this.gameParser = gameParser;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void send(Message msg) {
        if (channel != null && channel.isActive()) {
            @SuppressWarnings("unchecked")
            int msgId = gameParser.messageId((Class<Message>) msg.getClass());

            PushNotice push = PushNotice.newBuilder()
                    .addPlayerIds(playerId)
                    .setMsgId(msgId)
                    .setPayload(ByteString.copyFrom(msg.toByteArray()))
                    .build();

            channel.writeAndFlush(push);
        }
    }

}
