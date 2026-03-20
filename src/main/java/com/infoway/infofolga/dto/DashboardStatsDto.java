package com.infoway.infofolga.dto;
public record DashboardStatsDto(
    long pendingRequests,
    long totalEmployees,
    long approvedLast30Days,
    long rejectedLast30Days
) {}
