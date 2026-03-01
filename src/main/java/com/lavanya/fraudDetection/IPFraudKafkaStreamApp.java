package com.lavanya.fraudDetection;

import com.lavanya.fraudDetection.dashboard.service.MetricsService;
import com.lavanya.fraudDetection.lookup.CacheIPLookup;
import com.lavanya.fraudDetection.utils.PropertyReader;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Properties;

@Component
public class IPFraudKafkaStreamApp {

    private static final Logger logger =
            LoggerFactory.getLogger(IPFraudKafkaStreamApp.class);

    private final MetricsService metricsService;

    public IPFraudKafkaStreamApp(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @PostConstruct
    public void startStream() {

        PropertyReader propertyReader = new PropertyReader();
        CacheIPLookup cacheIPLookup = new CacheIPLookup();

        Properties props = new Properties();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG,
                propertyReader.getPropertyValue("application.id"));

        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                propertyReader.getPropertyValue("bootstrap.servers"));

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
                Serdes.String().getClass().getName());

        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
                Serdes.String().getClass().getName());

        StreamsBuilder builder = new StreamsBuilder();

        String inputTopic = propertyReader.getPropertyValue("topic");
        String outputTopic = propertyReader.getPropertyValue("output_topic");

        KStream<String, String> ipRecords = builder
                .stream(inputTopic, Consumed.with(Serdes.String(), Serdes.String()))
                .selectKey((key, value) -> {

                    if (value == null || value.trim().isEmpty()) {
                        logger.warn("Received null or empty record during key selection");
                        return key;
                    }

                    try {
                        String[] parts = value.split(" ");
                        if (parts.length == 0 || parts[0].trim().isEmpty()) {
                            logger.warn("Invalid record format: {}", value);
                            return key;
                        }

                        return parts[0];

                    } catch (Exception e) {
                        logger.error("Error extracting IP from record: {}", value, e);
                        return key;
                    }
                });

        // ✅ Count every log for dashboard
        ipRecords.peek((k, v) -> metricsService.incrementTotalLogs());

        KTable<Windowed<String>, Long> ipCounts =
                ipRecords.groupByKey()
                        .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(20)))
                        .count();

        KStream<String, String> suspiciousIPs =
                ipCounts.toStream()
                        .filter((windowedIp, count) -> count > 5)
                        .peek((windowedIp, count) ->
                                metricsService.incrementSuspicious(windowedIp.key()))
                        .map((windowedIp, count) ->
                                new KeyValue<>(
                                        windowedIp.key(),
                                        "Suspicious IP: " + windowedIp.key() +
                                                " | Requests: " + count
                                )
                        );

        KStream<String, String> fraudIpRecords =
                ipRecords
                        .filter((k, v) -> isFraud(v, cacheIPLookup))
                        .peek((k, v) -> metricsService.incrementFraudIP(k))
                        .map((k, v) ->
                                new KeyValue<>(k, "Fraud IP: " + k)
                        );

        KStream<String, String> mergedFraudStream =
                fraudIpRecords.merge(suspiciousIPs);

        mergedFraudStream.to(outputTopic);

        mergedFraudStream.foreach((key, value) ->
                logger.warn("{}", value)
        );

        KafkaStreams streams = new KafkaStreams(builder.build(), props);

        logger.info("Starting IP Fraud Detection Kafka Streams application...");
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private boolean isFraud(String record, CacheIPLookup cacheIPLookup) {

        if (record == null || record.trim().isEmpty()) {
            return false;
        }

        try {
            String IP = record.split(" ")[0];
            String[] parts = IP.split("\\.");

            if (parts.length < 1) {
                return false;
            }

            String firstOctet = parts[0];

            return cacheIPLookup.isFraudIP(firstOctet);

        } catch (Exception e) {
            return false;
        }
    }
}