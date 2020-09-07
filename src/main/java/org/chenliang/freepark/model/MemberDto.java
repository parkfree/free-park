package org.chenliang.freepark.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MemberDto {
    private Integer id;
    private Integer tenantId;
    private String userId;
    private String openId;
    private String memType;
    private String mobile;
    private LocalDate lastPaidAt;
}
