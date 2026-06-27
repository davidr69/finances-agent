package net.lavacro.finances.agent.kafka.config;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

public class ProtobufDeserializer<T extends Message> implements Deserializer<T> {

	private final Parser<T> parser;

	public ProtobufDeserializer(Parser<T> parser) {
		this.parser = parser;
	}

	@Override
	public T deserialize(String topic, byte[] data) {
		try {
			return parser.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new SerializationException("Error deserializing protobuf message", e);
		}
	}
}
