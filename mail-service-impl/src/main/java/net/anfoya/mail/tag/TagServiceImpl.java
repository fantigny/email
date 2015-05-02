package net.anfoya.mail.tag;

import java.util.List;

import net.anfoya.mail.model.Tag;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceException;
import net.anfoya.mail.service.TagService;
import net.anfoya.mail.service.TagServiceException;

public class TagServiceImpl implements TagService {

	private final MailService mailService;

	public TagServiceImpl(final MailService mailService) {
		this.mailService = mailService;
	}

	@Override
	public List<Tag> getTags() throws TagServiceException {
		try {
			return mailService.getTags();
		} catch (final MailServiceException e) {
			throw new TagServiceException("loading tags", e);
		}
	}
}
