package net.anfoya.mail.gmail.service;

import java.io.Serializable;

import com.google.api.services.gmail.model.Message;

@SuppressWarnings("serial")
public class SerializableMessage implements Serializable {
	private String id;
	private String raw;
	private transient Message message;
	public SerializableMessage() {}
	public SerializableMessage(final Message message) {
		this.message = message;
		id = message.getId();
		raw = message.getRaw();
	}
	public String getId() {
		return id;
	}
	public void setId(final String id) {
		this.id = id;
	}
	public String getRaw() {
		return raw;
	}
	public void setRaw(final String raw) {
		this.raw = raw;
	}
	public Message getMessage() {
		if (message == null) {
			message = new Message();
			message.setId(id);
			message.setRaw(raw);
		}
		return message;
	}
}
