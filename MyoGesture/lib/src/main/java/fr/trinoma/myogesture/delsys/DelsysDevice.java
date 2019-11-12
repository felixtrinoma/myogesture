package fr.trinoma.myogesture.delsys;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import fr.trinoma.daq.delsys.androidwrapper.ComponentInfo;
import fr.trinoma.myogesture.interfaces.device.BatteryPowered;
import fr.trinoma.myogesture.interfaces.device.Device;
import fr.trinoma.myogesture.interfaces.device.Mode;

public class DelsysDevice implements Device, BatteryPowered {

    private final static String VENDOR = "Delsys";

    private final String mId;
    private final float mBatteryLevel;
    private final String mModel;
    private final List<Mode> mSupportedModes;

    public DelsysDevice(
            String id,
            String model,
            List<Mode> supportedModes
    ) {
        mId = id;
        mBatteryLevel = Float.NaN; // TODO not implemented
        mModel = model;
        mSupportedModes = supportedModes;
    }

    public DelsysDevice(ComponentInfo componentInfo) {
        mId = componentInfo.getId();
        mModel = componentInfo.getName();
        mBatteryLevel = Float.NaN; // TODO not implemented
        mSupportedModes = new ArrayList<>(componentInfo.getModes().length);
        for (String modeDescription : componentInfo.getModes()) {
            mSupportedModes.add(DelsysMode.get(modeDescription));
        }
    }

    @Override
    public float getBatteryLevel() {
        return mBatteryLevel;
    }

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public String getVendor() {
        return VENDOR;
    }

    @Override
    public String getModel() {
        return mModel;
    }

    @Override
    public List<Mode> getSupportedModes() {
        return mSupportedModes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DelsysDevice that = (DelsysDevice) o;
        return Float.compare(that.mBatteryLevel, mBatteryLevel) == 0 &&
                Objects.equals(mId, that.mId) &&
                Objects.equals(mModel, that.mModel) &&
                Objects.equals(mSupportedModes, that.mSupportedModes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mBatteryLevel, mModel, mSupportedModes);
    }
}
