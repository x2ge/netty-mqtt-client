package io.x2ge.mqtt.core;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import io.x2ge.mqtt.MqttConnectOptions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ProtocolUtils {
    public static MqttConnectMessage connectMessage(MqttConnectOptions options) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNECT,
                false,
                MqttQoS.AT_MOST_ONCE,
                false,
                10);
        MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(
                options.getMqttVersion().protocolName(),
                options.getMqttVersion().protocolLevel(),
                options.isHasUserName(),
                options.isHasPassword(),
                options.isWillRetain(),
                options.getWillQos(),
                options.isWillFlag(),
                options.isCleanSession(),
                options.getKeepAliveTime());
        MqttConnectPayload payload = new MqttConnectPayload(
                options.getClientIdentifier(),
                options.getWillTopic(),
                options.getWillMessage(),
                options.getUserName(),
                options.getPassword());
        return new MqttConnectMessage(fixedHeader, variableHeader, payload);
    }

    public static MqttConnAckMessage connAckMessage(MqttConnectReturnCode returnCode, boolean sessionPresent) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNACK,
                false,
                MqttQoS.AT_MOST_ONCE,
                false,
                0);
        MqttConnAckVariableHeader variableHeader = new MqttConnAckVariableHeader(
                returnCode,
                sessionPresent);
        return (MqttConnAckMessage) MqttMessageFactory.newMessage(
                fixedHeader,
                variableHeader,
                null);
    }

    public static MqttConnectReturnCode connectReturnCodeForException(Throwable cause) {
        MqttConnectReturnCode code;
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

    public static List<String> getTopics(SubscriptionTopic[] subscriptionTopics) {
        if (subscriptionTopics != null) {
            List<String> topics = new LinkedList<>();
            for (SubscriptionTopic sb : subscriptionTopics) {
                topics.add(sb.getTopic());
            }
            return topics;
        } else {
            return null;
        }
    }

    public static List<MqttTopicSubscription> getTopicSubscriptions(SubscriptionTopic[] subscriptionTopics) {
        if (subscriptionTopics != null && subscriptionTopics.length > 0) {
            List<MqttTopicSubscription> list = new LinkedList<>();
            for (SubscriptionTopic sm : subscriptionTopics) {
                list.add(new MqttTopicSubscription(sm.getTopic(), MqttQoS.valueOf(sm.getQos())));
            }
            return list;
        }
        return null;
    }

    public static MqttSubscribeMessage subscribeMessage(int messageId, String... topics) {
        return subscribeMessage(messageId, 0, topics);
    }

    public static MqttSubscribeMessage subscribeMessage(int messageId, int qos, String... topics) {
        List<SubscriptionTopic> list = new ArrayList<>();
        for (String topic : topics) {
            SubscriptionTopic sb = new SubscriptionTopic();
            sb.setQos(qos);
            sb.setTopic(topic);
            list.add(sb);
        }
        return subscribeMessage(messageId, list.toArray(new SubscriptionTopic[0]));
    }

    public static MqttSubscribeMessage subscribeMessage(int messageId, SubscriptionTopic... subscriptionTopics) {
        return subscribeMessage(messageId, getTopicSubscriptions(subscriptionTopics));
    }

    public static MqttSubscribeMessage subscribeMessage(int messageId, List<MqttTopicSubscription> mqttTopicSubscriptions) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE,
                false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(messageId);
        MqttSubscribePayload mqttSubscribePayload = new MqttSubscribePayload(mqttTopicSubscriptions);
        return new MqttSubscribeMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttSubscribePayload);
    }

    public static MqttSubAckMessage subAckMessage(int messageId, List<Integer> mqttQoSList) {
        return (MqttSubAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId),
                new MqttSubAckPayload(mqttQoSList));
    }

    public static MqttUnsubscribeMessage unsubscribeMessage(int messageId, List<String> topicList) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_MOST_ONCE,
                false, 0x02);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(messageId);
        MqttUnsubscribePayload mqttUnsubscribeMessage = new MqttUnsubscribePayload(topicList);
        return new MqttUnsubscribeMessage(mqttFixedHeader, variableHeader, mqttUnsubscribeMessage);
    }

    public static MqttUnsubAckMessage unsubAckMessage(int messageId) {
        return (MqttUnsubAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId),
                null);
    }

    public static MqttMessage pingReqMessage() {
        return MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0),
                null,
                null);
    }

    public static MqttMessage pingRespMessage() {
        return MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0),
                null,
                null);
    }

    public static MqttPublishMessage publishMessage(MessageData mqttMessage) {
        return publishMessage(mqttMessage.getTopic(), mqttMessage.getPayload(), mqttMessage.getQos(),
                mqttMessage.isRetained(), mqttMessage.getMessageId(), mqttMessage.isDup());
    }

    public static MqttPublishMessage publishMessage(String topic, byte[] payload, int qosValue, int messageId, boolean isRetain) {
        return publishMessage(topic, payload, qosValue, isRetain, messageId, false);
    }

    public static MqttPublishMessage publishMessage(String topic, byte[] payload, int qosValue, boolean isRetain, int messageId, boolean isDup) {
        return (MqttPublishMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBLISH, isDup, MqttQoS.valueOf(qosValue), isRetain, 0),
                new MqttPublishVariableHeader(topic, messageId),
                Unpooled.buffer().writeBytes(payload));
    }

    public static MqttMessage pubCompMessage(int messageId) {
        return MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId),
                null);
    }

    public static MqttPubAckMessage pubAckMessage(int messageId) {
        return (MqttPubAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId),
                null);
    }

    public static MqttMessage pubRecMessage(int messageId) {
        return MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId),
                null);
    }

    public static MqttMessage pubRelMessage(int messageId, boolean isDup) {
        return MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBREL, isDup, MqttQoS.AT_LEAST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId),
                null);
    }
}