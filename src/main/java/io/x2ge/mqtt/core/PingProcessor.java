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

    public long lastReceivedAckTime;
    private final AtomicBoolean receivedAck = new AtomicBoolean(false);

    @Override
    public String call() throws Exception {
        while (!isCancelled()) {

            receivedAck.set(false);

            try {
                ping();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!isCancelled()) {
                synchronized (receivedAck) {
                    if (!receivedAck.get()) {
                        receivedAck.wait((TimeUnit.SECONDS.toMillis(keepAlive) / 2L));
                    }
                }
            }

            if (!isCancelled()) {
                if (isReceivedAckExpired()) {
                    TimeoutException te = new TimeoutException("Did not receive a response for a long time : " + keepAlive + "s");
                    if (cb != null) {
                        cb.onConnectLost(te);
                    }
                    throw te;
                }

                // 如果收到了ping应答，则在sleep指定时间后，再次ping
                if (receivedAck.get())
                    Thread.sleep(TimeUnit.SECONDS.toMillis(keepAlive));
            }
        }
        return null;
    }

    public void start(Channel channel, int keepAlive, Callback callback) {
        this.channel = channel;
        this.keepAlive = keepAlive;
        this.cb = callback;
        refreshLastReceivedAckTime();
        execute();
    }

    public void ping() throws Exception {
        MqttMessage msg = ProtocolUtils.pingReqMessage();
        Log.i("[ping]-->发起ping：" + msg);
        channel.writeAndFlush(msg);
    }

    /**
     * 收到服务器端应答，则调用该方法刷新收到应答的时间
     */
    public void refreshLastReceivedAckTime() {
        this.lastReceivedAckTime = System.nanoTime();
    }

    /**
     * 是否应答已过期，长时间未收到服务端消息
     *
     * @return 是否应答已过期
     */
    private boolean isReceivedAckExpired() {
        long l = System.nanoTime() - lastReceivedAckTime;
        long s = TimeUnit.NANOSECONDS.toSeconds(l);
        return s > keepAlive * 1.5f;
    }

    public void processAck(Channel channel, MqttMessage msg) {
        refreshLastReceivedAckTime();
        synchronized (receivedAck) {
            receivedAck.set(true);
            receivedAck.notify();
        }
    }

    public interface Callback {
        void onConnectLost(Throwable t);
    }

}
