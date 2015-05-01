package net.anfoya.mail.service;

import java.util.Set;

import net.anfoya.mail.model.Header;
import net.anfoya.mail.model.Tag;

public interface MailService {

	public void login(String id, String pwd);
	public void logout();
	public Set<Header> getHeaders(Set<Tag> tags);
}
