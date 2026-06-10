package com.barterplatform.application.notification.support;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class NotificationMetadataUtils {

    private NotificationMetadataUtils() {
    }

    public static Map<String, Object> metadataOf(Object... keyValuePairs) {
        if (keyValuePairs == null || keyValuePairs.length == 0) {
            return Map.of();
        }
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Notification metadata key/value arguments must come in pairs.");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            Object rawKey = keyValuePairs[i];
            if (!(rawKey instanceof String key) || key.isBlank()) {
                throw new IllegalArgumentException("Notification metadata keys must be non-blank strings.");
            }

            Object value = keyValuePairs[i + 1];
            if (value == null) {
                continue;
            }
            if (value instanceof String stringValue && stringValue.isBlank()) {
                continue;
            }
            if (value instanceof UUID uuidValue) {
                metadata.put(key, uuidValue.toString());
                continue;
            }

            metadata.put(key, value);
        }

        return metadata;
    }
}

