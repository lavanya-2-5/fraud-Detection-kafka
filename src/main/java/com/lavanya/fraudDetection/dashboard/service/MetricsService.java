package com.lavanya.fraudDetection.dashboard.service;

import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetricsService {

    private long totalLogs = 0;
    private long fraudCount = 0;
    private long suspiciousCount = 0;

    private final Map<String, Long> fraudIpCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> suspiciousIpCounts = new ConcurrentHashMap<>();

    private final List<Map<String, Object>> alerts = new ArrayList<>();
    private final List<Map<String, Object>> activity = new ArrayList<>();

    public synchronized void incrementTotalLogs() {
        totalLogs++;

        Map<String, Object> point = new HashMap<>();
        point.put("time", LocalTime.now().toString());
        point.put("count", totalLogs);
        activity.add(point);
    }

    public synchronized void incrementFraudIP(String ip) {
        fraudCount++;
        fraudIpCounts.merge(ip, 1L, Long::sum);

        addAlert("FRAUD", ip, "Fraud IP detected");
    }

    public synchronized void incrementSuspicious(String ip) {
        suspiciousCount++;
        suspiciousIpCounts.merge(ip, 1L, Long::sum);

        addAlert("SUSPICIOUS", ip, "Too many requests");
    }

    private void addAlert(String type, String ip, String details) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("time", LocalTime.now().toString());
        alert.put("type", type);
        alert.put("ip", ip);
        alert.put("details", details);

        alerts.add(0, alert);

        if (alerts.size() > 20) {
            alerts.remove(alerts.size() - 1);
        }
    }

    public Map<String, Object> getMetrics() {

        Map<String, Object> map = new HashMap<>();

        map.put("totalLogs", totalLogs);
        map.put("fraudCount", fraudCount);
        map.put("suspiciousCount", suspiciousCount);

        map.put("fraudRate",
                totalLogs == 0 ? 0 :
                        (double) fraudCount / totalLogs * 100);

        map.put("topFraudIPs", convertMapToList(fraudIpCounts));
        map.put("topSuspiciousIPs", convertMapToList(suspiciousIpCounts));

        map.put("activity", activity);
        map.put("alerts", alerts);

        return map;
    }

    private List<Map<String, Object>> convertMapToList(Map<String, Long> source) {
        List<Map<String, Object>> list = new ArrayList<>();

        source.forEach((ip, count) -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("ip", ip);
            entry.put("count", count);
            list.add(entry);
        });

        list.sort((a, b) ->
                Long.compare((Long) b.get("count"), (Long) a.get("count")));

        return list;
    }
}