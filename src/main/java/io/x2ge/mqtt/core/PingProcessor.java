package io.x2ge.mqtt.core;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.x2ge.mqtt.utils.AsyncTask;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PingProcessor extends AsyncTask<String> {

    public Channel channel;
    public int keepAlive = 60;
    public Callback cb;

    private boolean receivedAck = false;
    private Exception e;

    @Override
    public String call() throws Exception {
        while (!isCancelled()) {

            receivedAck = false;

            ping(channel);

            long start = System.nanoTime();
            while (!isCancelled()) {
                if (receivedAck) {
                    // 已经收到ping应答，跳出内循环，延时后进行下一次ping
                    break;
                }

                // 判断是否超时
                long l = System.nanoTime() - start;
                if (l > TimeUnit.SECONDS.toNanos(keepAlive) / 2) {
                    TimeoutException te = new TimeoutException("Did not receive a response for a long time : " + keepAlive + "s");
                    if (cb != null) {
                        cb.onConnectLost(te);
                    }
                    throw te;
                }

                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                    break;
                }
            }

            if (!isCancelled()) {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(keepAlive));
                } catch (InterruptedException ex) {
//                    ex.printStackTrace();
                    break;
                }
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
        channel.writeAndFlush(MqttProtocolUtil.pingReqMessage());
    }

    public void processAck(Channel channel, MqttMessage msg) {
        receivedAck = true;
    }

    public interface Callback {
        void onConnectLost(Throwable t);
    }

}
