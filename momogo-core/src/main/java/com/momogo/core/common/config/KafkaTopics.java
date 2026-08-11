package com.momogo.core.common.config;

/**
 * Kafka 토픽 이름 상수. 각자 문자열로 하드코딩하면 오타/불일치로 발행-구독이
 * 어긋날 수 있으므로, 새 토픽이 필요하면 이 클래스에 추가해서 공용으로 참조한다.
 */
public final class KafkaTopics {

    public static final String NOTIFICATION_EVENTS = "notification-events";
    public static final String AI_GRADING_EVENTS = "ai-grading-events";
    public static final String ROOM_SUBMIT_EVENTS = "room-submit-events";

    private KafkaTopics() {
    }
}
