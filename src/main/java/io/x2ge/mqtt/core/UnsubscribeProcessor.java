package io.x2ge.mqtt.core;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessageIdAndPropertiesVariableHeader;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.x2ge.mqtt.utils.AsyncTask;
import io.x2ge.mqtt.utils.Log;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UnsubscribeProcessor extends AsyncTask<String> {

    private long timeout;
    private int msgId;
    private final AtomicBoolean receivedAck = new AtomicBoolean(false);

    @Override
    public String call() throws Exception {
        if (!isCancelled() && !receivedAck.get()) {
            synchronized (receivedAck) {
                receivedAck.wait(timeout);
            }
        }
        return receivedAck.get() ? ProcessorResult.RESULT_SUCCESS : ProcessorResult.RESULT_FAIL;
    }

    public String unsubscribe(Channel channel, String[] topics, long timeout) throws Exception {
        this.timeout = timeout;

        int id = 0;
        String s;
        try {
            id = MessageIdFactory.get();

            this.msgId = id;

            MqttUnsubscribeMessage msg = ProtocolUtils.unsubscribeMessage(id, Arrays.asList(topics));
            Log.i("-->发起取消订阅：" + msg);
            channel.writeAndFlush(msg);
            s = execute().get(timeout, TimeUnit.MILLISECONDS);
        } finally {
            MessageIdFactory.release(id);
        }
        return s;
    }

    public void processAck(Channel channel, MqttUnsubAckMessage msg) {
        MqttMessageIdAndPropertiesVariableHeader variableHeader = msg.idAndPropertiesVariableHeader();
        if (variableHeader.messageId() == msgId) {
            synchronized (receivedAck) {
                receivedAck.set(true);
                receivedAck.notify();
            }
        }
    }
}
