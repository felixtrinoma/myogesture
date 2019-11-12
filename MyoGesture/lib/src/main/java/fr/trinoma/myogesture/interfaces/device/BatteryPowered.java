package fr.trinoma.myogesture.interfaces.device;

/**
 * Can be implemented by a battery powered {@link Device}.
 */
public interface BatteryPowered {
    float getBatteryLevel();
}
