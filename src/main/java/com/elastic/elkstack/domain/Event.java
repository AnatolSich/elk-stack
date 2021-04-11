package com.elastic.elkstack.domain;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class Event {
    private UUID id;
    private String title;
    private EventType type;
    private LocalDateTime datetime;
    private String place;
    private String description;
    private List<String> subTopics;
}
