package net.anfoya.mail.gmail.cache;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;

@SuppressWarnings("serial")
public class CacheData<T extends GenericJson> implements Serializable {

	private String json;
	private Class<T> clazz;

	private transient T t;

	public CacheData() {
	}

	@SuppressWarnings("unchecked")
	public CacheData(final T t) {
		this.json = t.toString();
		this.clazz = (Class<T>) t.getClass();
		this.t = t;
	}

	public T getData() throws IOException {
		if (t == null) {
			t = new JsonObjectParser(new JacksonFactory()).parseAndClose(new StringReader(json), clazz);
		}

		return t;
	}
}
