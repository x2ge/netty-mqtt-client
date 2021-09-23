package com.x2ge.mqtt;

import com.x2ge.mqtt.utils.AsyncTask;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.logging.LoggingHandler;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MqttClient {

    private MqttConnectOptions connectOptions;
    private long actionTimeout = 5000;
    private AsyncTask<String> connectTask;
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

    synchronized public void connect(MqttConnectOptions options) {
        if (connectTask != null)
            return;

        connectOptions = options;
        connectTask = new AsyncTask<String>() {
            @Override
            public String call() throws Exception {
                EventLoopGroup group = new NioEventLoopGroup();
                try {
                    Bootstrap b = new Bootstrap()
                            .group(group)
                            .channel(NioSocketChannel.class)
                            .handler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel channel) throws Exception {
                                    channel.pipeline()
                                            .addLast("log", new LoggingHandler())
                                            .addLast("decoder", new MqttDecoder())//解码
                                            .addLast("encoder", MqttEncoder.INSTANCE)//编码
                                            .addLast("handler", new Handler());
                                }
                            });
                    ChannelFuture ch = b.connect(options.getHost(), options.getPort()).sync();
                    channel = ch.channel();
                    Logger.getLogger("mqtt").log(Level.INFO, "--->" + channel.localAddress().toString());
                } catch (Exception e) {
//                    e.printStackTrace();
                    Logger.getLogger("mqtt").log(Level.INFO, "连接异常：" + e);
                    group.shutdownGracefully();
                    onConnectFailed(e);
                }

                if (channel == null)
                    return null;

                connect0(channel, options);

                try {
                    channel.closeFuture().sync();
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                    Logger.getLogger("mqtt").log(Level.INFO, "连接断开：" + e);
                    group.shutdownGracefully();
                }
                return null;
            }
        }.execute();
    }

    private void connect0(Channel channel, MqttConnectOptions options) {
        if (channel == null)
            return;

        try {
            connectProcessor = new ConnectProcessor();
            String s = connectProcessor.connect(channel, options, actionTimeout);
            if (ProcessorResult.RESULT_SUCCESS.equals(s)) {
                // 连接成功
                Logger.getLogger("mqtt").log(Level.INFO, "连接成功");
                onConnected();
            } else {
                // 连接取消
                Logger.getLogger("mqtt").log(Level.INFO, "连接取消");
            }
        } catch (Exception e) {
//            e.printStackTrace();
            // 连接取消
            // 连接取消、连接超时、连接异常
            if (e instanceof CancellationException) {
                // 连接取消
                Logger.getLogger("mqtt").log(Level.INFO, "连接取消");
            } else {
                Logger.getLogger("mqtt").log(Level.INFO, "连接异常：" + e);
                onConnectFailed(e);
            }
            close();
        }
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
                Logger.getLogger("mqtt").log(Level.INFO, "-->订阅成功：" + Arrays.toString(topics));
            } else {
                Logger.getLogger("mqtt").log(Level.INFO, "-->订阅取消：" + Arrays.toString(topics));
            }
        } catch (Exception e) {
//            e.printStackTrace();
            if (e instanceof CancellationException) {
                Logger.getLogger("mqtt").log(Level.INFO, "-->订阅取消：" + Arrays.toString(topics));
            } else {
                Logger.getLogger("mqtt").log(Level.INFO, "-->订阅异常：" + Arrays.toString(topics) + "    " + e);
                throw e;
            }
        }
        subscribeProcessorList.remove(sp);
    }

    public void unsubscribe(String... topics) throws Exception {
        UnsubscribeProcessor usp = new UnsubscribeProcessor();
        unsubscribeProcessorList.add(usp);
        try {
            String result = usp.unsubscribe(channel, topics, actionTimeout);
            if (ProcessorResult.RESULT_SUCCESS.equals(result)) {
                Logger.getLogger("mqtt").log(Level.INFO, "-->取消订阅成功：" + Arrays.toString(topics));
            } else {
                Logger.getLogger("mqtt").log(Level.INFO, "-->取消订阅取消：" + Arrays.toString(topics));
            }
        } catch (Exception e) {
//            e.printStackTrace();
            if (e instanceof CancellationException) {
                Logger.getLogger("mqtt").log(Level.INFO, "-->取消订阅取消：" + Arrays.toString(topics));
            } else {
                Logger.getLogger("mqtt").log(Level.INFO, "-->取消订阅异常：" + Arrays.toString(topics) + "    " + e);
                throw e;
            }
        }
        unsubscribeProcessorList.remove(usp);
    }

    public void publish(String topic, String content) throws Exception {
        PublishProcessor pp = new PublishProcessor();
        publishProcessorList.add(pp);
        try {
            String result = pp.publish(channel, topic, content, actionTimeout);
            if (ProcessorResult.RESULT_SUCCESS.equals(result)) {
                Logger.getLogger("mqtt").log(Level.INFO, "-->发布成功：" + content);
            } else {
                Logger.getLogger("mqtt").log(Level.INFO, "-->发布取消：" + content);
            }
        } catch (Exception e) {
//            e.printStackTrace();
            if (e instanceof CancellationException) {
                Logger.getLogger("mqtt").log(Level.INFO, "发布取消：" + content);
            } else {
                Logger.getLogger("mqtt").log(Level.INFO, "-->发布异常：" + content + "    " + e);
                throw e;
            }
        }
        publishProcessorList.remove(pp);
    }

    public void disConnect() throws Exception {
        if (channel != null) {
            channel.writeAndFlush(MqttProtocolUtil.disConnectMessage());
        }
    }

    public void close() {
        setConnected(false);
        setClosed(true);

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
        if (callback != null) {
            callback.onConnectLost(t);
        }
    }

    private void onMessageArrived(String topic, String s) {
        Logger.getLogger("mqtt").log(Level.INFO, "-->收到消息：" + topic + " | " + s);
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
            Logger.getLogger("mqtt").log(Level.INFO, "--channelRead0-->" + msgx);

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
            Logger.getLogger("mqtt").log(Level.INFO, "-->发生异常：" + t);
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



