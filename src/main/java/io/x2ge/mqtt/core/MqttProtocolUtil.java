package io.x2ge.mqtt.core;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import io.x2ge.mqtt.MqttConnectOptions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MqttProtocolUtil {
    public static MqttConnectMessage connectMessage(MqttConnectOptions options) {
        MqttVersion version = options.getMqttVersion();
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE,
                false, 10);
        MqttConnectVariableHeader mqttConnectVariableHeader = new MqttConnectVariableHeader(version.protocolName(),
                version.protocolLevel(), options.isHasUserName(), options.isHasPassword(), options.isWillRetain(),
                options.getWillQos(), options.isWillFlag(), options.isCleanSession(), options.getKeepAliveTime());
        MqttConnectPayload mqttConnectPayload = new MqttConnectPayload(options.getClientIdentifier(), options.getWillTopic(),
                options.getWillMessage(), options.getUserName(), options.getPassword());
        return new MqttConnectMessage(mqttFixedHeader, mqttConnectVariableHeader, mqttConnectPayload);
    }

    public static MqttConnAckMessage connAckMessage(MqttConnectReturnCode code, boolean sessionPresent) {
        return (MqttConnAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                new MqttConnAckVariableHeader(code, sessionPresent), null);
    }

    public static MqttConnectReturnCode connectReturnCodeForException(Throwable cause) {
        MqttConnectReturnCode code = MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE;
        if (cause instanceof MqttUnacceptableProtocolVersionException) {
            // 不支持的协议版本
            code = MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;
        } else if (cause instanceof MqttIdentifierRejectedException) {
            // 不合格的clientId
            code = MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
        } else {
            code = MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE;
        }
        return code;
    }

    public static MqttMessage disConnectMessage() {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE,
                false, 0x02);
        return new MqttMessage(mqttFixedHeader);
    }

    public static List<String> getTopics(SubscribeMessage[] sMsgObj) {
        if (sMsgObj != null) {
            List<String> topics = new LinkedList<>();
            for (SubscribeMessage sb : sMsgObj) {
                topics.add(sb.getTopic());
            }
            return topics;
        } else {
            return null;
        }
    }

    public static List<MqttTopicSubscription> getSubscribeTopics(SubscribeMessage[] sbs) {
        if (sbs != null) {
            List<MqttTopicSubscription> list = new LinkedList<>();
            for (SubscribeMessage sb : sbs) {
                MqttTopicSubscription mqttTopicSubscription = new MqttTopicSubscription(sb.getTopic(),
                        MqttQoS.valueOf(sb.getQos()));
                list.add(mqttTopicSubscription);
            }
            return list;
        } else {
            return null;
        }
    }

    public static MqttSubscribeMessage subscribeMessage(int messageId, String... topics) {
        List<SubscribeMessage> list = new ArrayList<>();
        for (String s : topics) {
            SubscribeMessage sb = new SubscribeMessage();
            sb.setTopic(s);
            list.add(sb);
        }
        return subscribeMessage(messageId, list.toArray(new SubscribeMessage[0]));
    }

    public static MqttSubscribeMessage subscribeMessage(int messageId, SubscribeMessage... msg) {
        return subscribeMessage(messageId, getSubscribeTopics(msg));
    }

    public static MqttSubscribeMessage subscribeMessage(int messageId, List<MqttTopicSubscription> mqttTopicSubscriptions) {
        MqttSubscribePayload mqttSubscribePayload = new MqttSubscribePayload(mqttTopicSubscriptions);
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE,
                false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        return new MqttSubscribeMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttSubscribePayload);
    }

    public static MqttSubAckMessage subAckMessage(int messageId, List<Integer> mqttQoSList) {
        return (MqttSubAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId), new MqttSubAckPayload(mqttQoSList));
    }

    public static MqttUnsubscribeMessage unsubscribeMessage(int messageId, List<String> topic) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_MOST_ONCE,
                false, 0x02);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(messageId);
        MqttUnsubscribePayload mqttUnsubscribeMessage = new MqttUnsubscribePayload(topic);
        return new MqttUnsubscribeMessage(mqttFixedHeader, variableHeader, mqttUnsubscribeMessage);
    }

    public static MqttUnsubAckMessage unsubAckMessage(int messageId) {
        return (MqttUnsubAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId), null);
    }

    public static MqttMessage pingReqMessage() {
        return MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0), null, null);
    }

    public static MqttMessage pingRespMessage() {
        return MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0), null, null);
    }

    public static MqttPublishMessage publishMessage(MessageData mqttMessage) {
        return publishMessage(mqttMessage.getTopic(), mqttMessage.isDup(), mqttMessage.getQos(),
                mqttMessage.isRetained(), mqttMessage.getMessageId(), mqttMessage.getPayload());
    }

    public static MqttPublishMessage publishMessage(String topic, byte[] payload, int qosValue, int messageId,
                                                    boolean isRetain) {
        return publishMessage(topic, false, qosValue, isRetain, messageId, payload);
    }

    public static MqttPublishMessage publishMessage(String topic, byte[] payload, int qosValue, boolean isRetain,
                                                    int messageId, boolean isDup) {
        return publishMessage(topic, isDup, qosValue, isRetain, messageId, payload);
    }

    public static MqttPublishMessage publishMessage(String topicName, boolean isDup, int qosValue, boolean isRetain,
                                                    int messageId, byte[] payload) {
        return (MqttPublishMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBLISH, isDup, MqttQoS.valueOf(qosValue), isRetain, 0),
                new MqttPublishVariableHeader(topicName, messageId), Unpooled.buffer().writeBytes(payload));
    }

    public static MqttMessage pubCompMessage(int messageId) {
        return MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId), null);
    }

    public static MqttPubAckMessage pubAckMessage(int messageId) {
        return (MqttPubAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId), null);
    }

    public static MqttMessage pubRecMessage(int messageId) {
        return MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId), null);
    }

    public static MqttMessage pubRelMessage(int messageId, boolean isDup) {
        return MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBREL, isDup, MqttQoS.AT_LEAST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId), null);
    }
}