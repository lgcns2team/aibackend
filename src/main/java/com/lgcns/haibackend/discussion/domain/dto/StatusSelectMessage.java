package com.lgcns.haibackend.discussion.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusSelectMessage {
    private java.util.UUID userId;
    private DebateStatus status;
}
