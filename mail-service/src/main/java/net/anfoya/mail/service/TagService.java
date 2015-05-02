package net.anfoya.mail.service;

import java.util.List;

import net.anfoya.mail.model.Tag;

public interface TagService {

	public List<Tag> getTags() throws TagServiceException;
}
