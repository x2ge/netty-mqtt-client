package io.x2ge.mqtt.core;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.x2ge.mqtt.utils.AsyncTask;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class PingProcessor extends AsyncTask<String> {

    public Channel channel;
    public int keepAlive = 60;
    public Callback cb;

    private final AtomicBoolean acked = new AtomicBoolean(false);
    private Exception e;

    @Override
    public String call() throws Exception {
        while (!isCancelled()) {

            acked.set(false);

            ping(channel);

            long start = System.nanoTime();
            while (!isCancelled()) {
                if (acked.get()) {
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

                synchronized (acked) {
                    try {
                        acked.wait(300L);
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
        channel.writeAndFlush(MqttProtocolUtil.pingReqMessage());
    }

    public void processAck(Channel channel, MqttMessage msg) {
        synchronized (acked) {
            acked.set(true);
            acked.notify();
        }
    }

    public interface Callback {
        void onConnectLost(Throwable t);
    }

}
