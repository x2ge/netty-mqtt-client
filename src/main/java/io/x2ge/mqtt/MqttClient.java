package io.x2ge.mqtt;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.*;
import io.x2ge.mqtt.core.*;
import io.x2ge.mqtt.utils.AsyncTask;
import io.x2ge.mqtt.utils.Log;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

public class MqttClient {

    private MqttConnectOptions connectOptions;
    private long actionTimeout = 5000;
    private long connectTimeout = 5000;

    private int maxReconnectTimesOnLost = 0;
    private long reconnectTimeoutOnLost = 0;
    private final static long MIN_RECONNECT_INTERVAL = 1800L;

    private AsyncTask<String> connectTask;
    private AsyncTask<String> reconnectTask;
    private Channel channel;

    private ConnectProcessor connectProcessor;
    private PingProcessor pingProcessor;
    private List<SubscribeProcessor> subscribeProcessorList = new ArrayList<>();
    private List<UnsubscribeProcessor> unsubscribeProcessorList = new ArrayList<>();
    private List<PublishProcessor> publishProcessorList = new ArrayList<>();

    private boolean isConnected = false;
    private boolean isClosed = false;

    private Callback callback;

    public void setCallback(Callback c) {
        this.callback = c;
    }

    /**
     * 设置连接、订阅、取消订阅、发布消息、ping等动作的超时时间
     *
     * @param actionTimeout 等待动作完成的超时时间
     */
    public void setActionTimeout(long actionTimeout) {
        this.actionTimeout = actionTimeout;
    }

    /**
     * 当maxTimes大于0时，如果发生掉线，则自动尝试重连，重连成功则回调onConnected方法，
     * 重连次数用完则回调onConnectLost方法。
     * 当timeout大于0时，如果整个重连过程消耗时间超过timeout，此时无论重连次数是否用完都
     * 停止重试，并回调onConnectLost方法。
     *
     * @param maxTimes 重试最大次数
     * @param timeout  重试超时时间
     */
    public void setReconnectOnLost(int maxTimes, long timeout) {
        this.maxReconnectTimesOnLost = maxTimes;
        this.reconnectTimeoutOnLost = timeout;
    }

    synchronized public void connect(MqttConnectOptions options) throws Exception {
        connect(options, actionTimeout);
    }

    synchronized public void connect(MqttConnectOptions options, long timeout) throws Exception {
        if (this.connectOptions != null) {
            return;
        }
        this.connectOptions = options;
        this.connectTimeout = timeout;

        try {
            doConnect(options, timeout);
            onConnected();
        } catch (Exception e) {
//            e.printStackTrace();
            onConnectFailed(e);
            throw e;
        }
    }

    private void doConnect(MqttConnectOptions options, long timeout) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        connectTask = new AsyncTask<String>() {
            @Override
            public String call() throws Exception {
                Bootstrap b = new Bootstrap()
                        .group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel channel) throws Exception {
                                channel.pipeline()
                                        .addLast("decoder", new MqttDecoder())//解码
                                        .addLast("encoder", MqttEncoder.INSTANCE)//编码
                                        .addLast("handler", new Handler());
                            }
                        });
                ChannelFuture ch = b.connect(options.getHost(), options.getPort()).sync();
                channel = ch.channel();
                Log.i("--已连接->" + channel.localAddress().toString());
                return null;
            }
        }.execute();
        try {
            connectTask.get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
//            e.printStackTrace();
            Log.i("连接异常：" + e);
            group.shutdownGracefully();
            throw e;
        }

        if (channel == null)
            return;

        doConnect0(channel, options, timeout);

        connectTask = new AsyncTask<String>() {
            @Override
            public String call() throws Exception {
                try {
                    channel.closeFuture().sync();
                } catch (Exception e) {
//                    e.printStackTrace();
                    Log.i("连接断开：" + e);
                    group.shutdownGracefully();
                }
                return null;
            }
        }.execute();
    }

    private void doConnect0(Channel channel, MqttConnectOptions options, long timeout) throws Exception {
        if (channel == null)
            return;

        try {
            connectProcessor = new ConnectProcessor();
            String s = connectProcessor.connect(channel, options, timeout);
            if (ProcessorResult.RESULT_SUCCESS.equals(s)) {
                Log.i("-->连接成功");
            } else {
                throw new CancellationException();
            }
        } catch (Exception e) {
//            e.printStackTrace();
            if (e instanceof CancellationException) {
                Log.i("-->连接取消");
            } else {
                Log.i("-->连接异常：" + e);
                throw e;
            }
        }
    }

    private void doReconnect(MqttConnectOptions options, final int maxTimes, final long timeout, Throwable t) {
        reconnectTask = new AsyncTask<String>() {
            @Override
            public String call() throws Exception {
                long interval = MIN_RECONNECT_INTERVAL;
                if (timeout > 0) {
                    interval = timeout / maxTimes;
                    if (interval < MIN_RECONNECT_INTERVAL)
                        interval = MIN_RECONNECT_INTERVAL;
                }

                boolean bSuccess = false;
                int num = 0;
                long start = System.nanoTime();
                do {
                    ++num;
                    Log.i("-->重连开始：" + num);
                    onReconnectStart(num);

                    long begin = System.nanoTime();
                    try {
                        doConnect(options, interval);
                        Log.i("<--重连成功：" + num);
                        bSuccess = true;
                        break;
                    } catch (Exception e) {
//                        e.printStackTrace();
                        Log.i("<--重连失败：" + num);
                    }

                    if (maxTimes <= num) { // 重试次数已经消耗殆尽
                        break;
                    }

                    // 判断是否timeout
                    if (timeout > 0) { // 只在配置了重连超时时间情况下才进行相关判断
                        // 重连总消耗时间
                        long spendTotal = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                        if (timeout <= spendTotal) {// 超时时间已经消耗殆尽
                            break;
                        }
                    }

                    // 单次连接消耗时间
                    long spend = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin);
                    long sleepTime = interval - spend;
                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
//                            e.printStackTrace();
                            break;
                        }
                    }
                } while (!isCancelled());

                if (!isCancelled()) {
                    if (bSuccess) {
                        onConnected();
                    } else {
                        onReconnectFailed(t);
                    }
                }
                return null;
            }
        }.execute();
    }


    private void startPingTask(Channel channel, int keepAliveTime) {
        if (pingProcessor == null
                || pingProcessor.isCancelled()
                || pingProcessor.isDone())
            pingProcessor = new PingProcessor();
        pingProcessor.start(channel, keepAliveTime, new PingCallback());
    }

    public void subscribe(String... topics) throws Exception {
        SubscribeProcessor sp = new SubscribeProcessor();
        subscribeProcessorList.add(sp);
        try {
            String result = sp.subscribe(channel, topics, actionTimeout);
            if (ProcessorResult.RESULT_SUCCESS.equals(result)) {
                Log.i("-->订阅成功：" + Arrays.toString(topics));
            } else {
                throw new CancellationException();
            }
        } catch (Exception e) {
//            e.printStackTrace();
            if (e instanceof CancellationException) {
                Log.i("-->订阅取消：" + Arrays.toString(topics));
            } else {
                Log.i("-->订阅异常：" + Arrays.toString(topics) + "    " + e);
                throw e;
            }
        } finally {
            subscribeProcessorList.remove(sp);
        }
    }

    public void unsubscribe(String... topics) throws Exception {
        UnsubscribeProcessor usp = new UnsubscribeProcessor();
        unsubscribeProcessorList.add(usp);
        try {
            String result = usp.unsubscribe(channel, topics, actionTimeout);
            if (ProcessorResult.RESULT_SUCCESS.equals(result)) {
                Log.i("-->取消订阅成功：" + Arrays.toString(topics));
            } else {
                throw new CancellationException();
            }
        } catch (Exception e) {
//            e.printStackTrace();
            if (e instanceof CancellationException) {
                Log.i("-->取消订阅取消：" + Arrays.toString(topics));
            } else {
                Log.i("-->取消订阅异常：" + Arrays.toString(topics) + "    " + e);
                throw e;
            }
        } finally {
            unsubscribeProcessorList.remove(usp);
        }
    }

    public void publish(String topic, String content) throws Exception {
        PublishProcessor pp = new PublishProcessor();
        publishProcessorList.add(pp);
        try {
            String result = pp.publish(channel, topic, content, actionTimeout);
            if (ProcessorResult.RESULT_SUCCESS.equals(result)) {
                Log.i("-->发布成功：" + content);
            } else {
                throw new CancellationException();
            }
        } catch (Exception e) {
//            e.printStackTrace();
            if (e instanceof CancellationException) {
                Log.i("-->发布取消：" + content);
            } else {
                Log.i("-->发布异常：" + content + "    " + e);
                throw e;
            }
        } finally {
            publishProcessorList.remove(pp);
        }
    }

    public void disConnect() throws Exception {
        if (channel != null) {
            channel.writeAndFlush(MqttProtocolUtil.disConnectMessage());
        }
    }

    public void close() {
        setConnected(false);
        setClosed(true);

        if (reconnectTask != null)
            reconnectTask.cancel(true);

        if (connectProcessor != null) {
            connectProcessor.cancel(true);
        }

        if (pingProcessor != null) {
            pingProcessor.cancel(true);
        }

        if (subscribeProcessorList.size() > 0) {
            for (SubscribeProcessor sp : subscribeProcessorList) {
                sp.cancel(true);
            }
        }

        if (unsubscribeProcessorList.size() > 0) {
            for (UnsubscribeProcessor usp : unsubscribeProcessorList) {
                usp.cancel(true);
            }
        }

        if (publishProcessorList.size() > 0) {
            for (PublishProcessor pp : publishProcessorList) {
                pp.cancel(true);
            }
        }

        if (channel != null) {
            try {
                disConnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                channel.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            channel = null;
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    private void setConnected(boolean b) {
        isConnected = b;
    }

    public boolean isClosed() {
        return isClosed;
    }

    private void setClosed(boolean b) {
        isClosed = b;
    }

    private void onConnected() {
        setConnected(true);
        startPingTask(channel, connectOptions.getKeepAliveTime());
        if (callback != null)
            callback.onConnected();
    }

    private void onConnectFailed(Throwable t) {
        close();
        if (callback != null)
            callback.onConnectFailed(t);
    }

    private void onConnectLost(Throwable t) {
        close();

        if (maxReconnectTimesOnLost > 0) {
            doReconnect(connectOptions, maxReconnectTimesOnLost, reconnectTimeoutOnLost, t);
        } else {
            if (callback != null) {
                callback.onConnectLost(t);
            }
        }
    }

    private void onReconnectStart(int num) {
        if (callback != null)
            callback.onReconnectStart(num);
    }

    private void onReconnectFailed(Throwable t) {
        close();
        if (callback != null)
            callback.onConnectLost(t);
    }

    private void onMessageArrived(String topic, String s) {
        Log.i("-->收到消息：" + topic + " | " + s);
        if (callback != null) {
            callback.onMessageArrived(topic, s);
        }
    }

    class Handler extends SimpleChannelInboundHandler<Object> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msgx) throws Exception {
            if (msgx == null) {
                return;
            }
            Log.i("--channelRead0-->" + msgx);

            MqttMessage msg = (MqttMessage) msgx;
            MqttFixedHeader mqttFixedHeader = msg.fixedHeader();
            switch (mqttFixedHeader.messageType()) {
                case CONNACK:
                    if (connectProcessor != null)
                        connectProcessor.processAck(ctx.channel(), (MqttConnAckMessage) msg);
                    break;
                case SUBACK:
                    if (subscribeProcessorList.size() > 0) {
                        for (SubscribeProcessor subscribeProcessor : subscribeProcessorList) {
                            subscribeProcessor.processAck(ctx.channel(), (MqttSubAckMessage) msg);
                        }
                    }
                    break;
                case UNSUBACK:
                    if (unsubscribeProcessorList.size() > 0) {
                        for (UnsubscribeProcessor unsubscribeProcessor : unsubscribeProcessorList) {
                            unsubscribeProcessor.processAck(ctx.channel(), (MqttUnsubAckMessage) msg);
                        }
                    }
                    break;
                case PUBLISH:
                    MqttPublishMessage publishMessage = (MqttPublishMessage) msg;
                    MqttPublishVariableHeader mqttPublishVariableHeader = publishMessage.variableHeader();
                    String topicName = mqttPublishVariableHeader.topicName();
                    ByteBuf payload = publishMessage.payload();

                    onMessageArrived(topicName, payload.toString(StandardCharsets.UTF_8));
                    break;
                case PUBACK:
                    // qos = 1的发布才有该响应
                    if (publishProcessorList.size() > 0) {
                        for (PublishProcessor publishProcessor : publishProcessorList) {
                            publishProcessor.processAck(ctx.channel(), (MqttPubAckMessage) msg);
                        }
                    }
                    break;
                case PUBREC:
                    // qos = 2的发布才参与
                    break;
                case PUBREL:
                    // qos = 2的发布才参与
                    break;
                case PUBCOMP:
                    // qos = 2的发布才参与
                    break;
                case PINGRESP:
                    if (pingProcessor != null) {
                        pingProcessor.processAck(ctx.channel(), msg);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    class PingCallback implements PingProcessor.Callback {

        @Override
        public void onConnectLost(Throwable t) {
            Log.i("-->发生异常：" + t);
            MqttClient.this.onConnectLost(t);
        }
    }

    public interface Callback {

        void onConnected();

        void onConnectFailed(Throwable e);

        void onConnectLost(Throwable e);

        /**
         * @param cur 第几次重连
         */
        void onReconnectStart(int cur);

        void onMessageArrived(String topic, String s);
    }

}



