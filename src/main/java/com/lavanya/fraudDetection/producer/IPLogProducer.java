package com.lavanya.fraudDetection.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import com.lavanya.fraudDetection.utils.PropertyReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class IPLogProducer {

    public static void main(String[] args) throws InterruptedException {

        PropertyReader propertyReader = new PropertyReader();
        String topic = propertyReader.getPropertyValue("topic");

        // Kafka producer properties
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers",
                propertyReader.getPropertyValue("bootstrap.servers"));
        producerProps.put("key.serializer",
                "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer",
                "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);

        // This fix handles the 'Resource Leak' warning by closing the producer on exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Closing Kafka Producer...");
            producer.close();
        }));

        while (true) {
    try (BufferedReader reader = new BufferedReader(new FileReader("IP_LOG.log"))) {
        String line;
        while ((line = reader.readLine()) != null) {

            ProducerRecord<String, String> record = new ProducerRecord<>(topic, line);

            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    System.out.println("Sent to topic: " + metadata.topic() +
                            " | partition: " + metadata.partition());
                } else {
                    exception.printStackTrace();
                }
            });

            Thread.sleep(1000);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}
    }
}