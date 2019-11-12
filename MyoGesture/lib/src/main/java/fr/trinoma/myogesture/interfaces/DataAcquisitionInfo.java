package fr.trinoma.myogesture.interfaces;

import java.util.LinkedList;
import java.util.List;

import fr.trinoma.myogesture.interfaces.device.Device;
import fr.trinoma.myogesture.interfaces.signal.ChannelReader;

public class DataAcquisitionInfo {

    int minimumBufferSize;
    final List<Signal> signals = new LinkedList<>();

    public int getMinimumBufferSize() {
        return minimumBufferSize;
    }

    public void setMinimumBufferSize(int size) {
        minimumBufferSize = size;
    }

    public static class Signal {
        private final Device device;
        private final ChannelReader reader;
        private final String description;

        Signal(Device device, ChannelReader reader, String description) {
            this.device = device;
            this.reader = reader;
            this.description = description;
        }

        public Device getDevice() {
            return device;
        }

        public ChannelReader getReader() {
            return reader;
        }

        public String getDescription() {
            return description;
        }
    }

    public void addSignal(Device device, ChannelReader reader, String description) {
        addSignal(new Signal(device, reader, description));
    }

    public void addSignal(Signal signal) {
        signals.add(signal);
    }

    public List<Signal> getSignals() {
        return signals;
    }
}
