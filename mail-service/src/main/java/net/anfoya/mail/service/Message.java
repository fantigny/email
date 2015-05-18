package net.anfoya.mail.service;

import java.io.Serializable;

public interface Message extends Serializable {

	public String getId();
	public byte[] getRfc822mimeRaw();
	public boolean isDraft();
}
