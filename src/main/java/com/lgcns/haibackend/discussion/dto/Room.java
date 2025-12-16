package com.lgcns.haibackend.discussion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Room {
    private String roomId;
    private UUID teacherId;
    private Integer grade;
    private Integer classNumber;
}
