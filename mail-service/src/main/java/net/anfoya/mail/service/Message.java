package net.anfoya.mail.service;

import java.io.Serializable;

public interface Message extends Serializable {

	public String getId();
	public boolean isDraft();
	public byte[] getRaw();
	public void setRaw(byte[] rfc822mimeRaw);
}
