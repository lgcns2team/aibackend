package com.lgcns.haibackend.discussion.domain.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusSelectMessage {
    private UUID userId;
    private DebateStatus status;
}
