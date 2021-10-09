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
    private Exception e;

    @Override
    public String call() throws Exception {
        while (!isCancelled()) {

            receivedAck.set(false);

            ping(channel);

            long start = System.nanoTime();
            while (!isCancelled()) {
                if (receivedAck.get()) {
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

                synchronized (receivedAck) {
                    try {
                        receivedAck.wait(300L);
                    } catch (Exception ex) {
//                        ex.printStackTrace();
                    }
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
