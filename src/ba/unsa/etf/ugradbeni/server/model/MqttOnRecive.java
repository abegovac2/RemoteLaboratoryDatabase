package ba.unsa.etf.ugradbeni.server.model;

import org.eclipse.paho.client.mqttv3.MqttMessage;

@FunctionalInterface
public interface MqttOnRecive {
    void execute(String topic, MqttMessage message);
}
