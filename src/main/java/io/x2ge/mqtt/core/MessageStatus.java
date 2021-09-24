package io.x2ge.mqtt.core;

public enum MessageStatus {
    /**
     * none
     */
    None,
    /**
     * Qos1
     */
    PUB,
    /**
     * Qos2
     */
    PUBREC,
    /**
     * Qos2
     */
    PUBREL,
    /**
     * finish
     */
    COMPLETE,
}
