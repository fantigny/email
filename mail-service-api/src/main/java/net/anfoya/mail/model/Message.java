package net.anfoya.mail.model;

import java.io.Serializable;

import javax.mail.internet.MimeMessage;

public interface Message extends Serializable {

	public String getId();
	public boolean isDraft();
	public String getSnippet();

	public MimeMessage getMimeMessage();
	public void setMimeDraft(MimeMessage mimeDraft);
}
