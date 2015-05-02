package net.anfoya.mail.service;

import java.util.List;

import net.anfoya.tag.model.Tag;

public interface MailService {

	public void login(String id, String pwd) throws MailServiceException;
	public void logout();
	public List<net.anfoya.mail.model.Thread> getThreads(List<Tag> tags) throws MailServiceException;
	public List<Tag> getTags() throws MailServiceException;
	public String getMail(String mailId);
}
