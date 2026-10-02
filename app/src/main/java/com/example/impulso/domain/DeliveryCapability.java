package com.example.impulso.domain;

/** Separa el deseo de la persona de lo que el sistema puede entregar ahora. */
public final class DeliveryCapability {
    public static final String OFF = "off";
    public static final String BLOCKED = "blocked";
    public static final String EXACT = "exact";
    public static final String APPROXIMATE = "approximate";

    private DeliveryCapability() {
    }

    public static String resolve(
            boolean desireOn,
            boolean notificationsEnabled,
            boolean channelEnabled,
            boolean wantsExact,
            boolean canExact) {
        if (!desireOn) {
            return OFF;
        }
        if (!notificationsEnabled || !channelEnabled) {
            return BLOCKED;
        }
        if (wantsExact && canExact) {
            return EXACT;
        }
        return APPROXIMATE;
    }
}
