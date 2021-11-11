package io.x2ge.mqtt.core;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.x2ge.mqtt.utils.AsyncTask;
import io.x2ge.mqtt.utils.Log;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class PingProcessor extends AsyncTask<String> {

    public Channel channel;
    public int keepAlive = 60;
    public Callback cb;

    private final AtomicBoolean receivedAck = new AtomicBoolean(false);

    @Override
    public String call() throws Exception {
        while (!isCancelled()) {

            receivedAck.set(false);

            ping(channel);

            if (!isCancelled() && !receivedAck.get()) {
                synchronized (receivedAck) {
                    receivedAck.wait(TimeUnit.SECONDS.toMillis(keepAlive) / 2);
                }
            }

            if (!isCancelled()) {
                if (!receivedAck.get()) {
                    TimeoutException te = new TimeoutException("Did not receive a response for a long time : " + keepAlive + "s");
                    if (cb != null) {
                        cb.onConnectLost(te);
                    }
                    throw te;
                }

                Thread.sleep(TimeUnit.SECONDS.toMillis(keepAlive));
            }
        }
        return null;
    }

    public void start(Channel channel, int keepAlive, Callback callback) {
        this.channel = channel;
        this.keepAlive = keepAlive;
        this.cb = callback;
        execute();
    }

    public void ping(Channel channel) throws Exception {
        MqttMessage msg = ProtocolUtils.pingReqMessage();
        Log.i("[ping]-->发起ping：" + msg);
        channel.writeAndFlush(msg);
    }

    public void processAck(Channel channel, MqttMessage msg) {
        synchronized (receivedAck) {
            receivedAck.set(true);
            receivedAck.notify();
        }
    }

    public interface Callback {
        void onConnectLost(Throwable t);
    }

}
