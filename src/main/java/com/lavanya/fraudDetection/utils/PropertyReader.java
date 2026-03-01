package com.lavanya.fraudDetection.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyReader {

    private static final Logger logger =
            LoggerFactory.getLogger(PropertyReader.class);

    private final Properties prop;

    public PropertyReader() {

        prop = new Properties();

        try (InputStream is =
                     this.getClass().getResourceAsStream("/streaming.properties")) {

            if (is == null) {
                throw new RuntimeException("streaming.properties file not found in resources folder");
            }

            prop.load(is);

        } catch (IOException e) {
            logger.error("Error loading streaming.properties file", e);
            throw new RuntimeException("Failed to load configuration file", e);
        }
    }

    public String getPropertyValue(String key) {
        return prop.getProperty(key);
    }
}