package net.anfoya.mail.tag;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceException;
import net.anfoya.mail.service.MailTagService;
import net.anfoya.tag.model.Section;
import net.anfoya.tag.model.Tag;
import net.anfoya.tag.service.TagService;
import net.anfoya.tag.service.TagServiceException;

public class TagServiceImpl implements MailTagService, TagService {

	private final MailService mailService;

	public TagServiceImpl(final MailService mailService) {
		this.mailService = mailService;
	}

	@SuppressWarnings("serial")
	@Override
	public Set<Section> getSections() {
		return new LinkedHashSet<Section>() {{ add(new Section("test section")); }};
	}

	@Override
	public List<Tag> getTags(final Section section, final String tagPattern) throws TagServiceException {
		try {
			return mailService.getTags();
		} catch (final MailServiceException e) {
			throw new TagServiceException("", e);
		}
	}

	@Override
	public void addToSection(final Tag tag) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getCount(final Set<Tag> includes, final Set<Tag> excludes, final String pattern) {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public int getSectionCount(final Section section, final Set<Tag> includes,
			final Set<Tag> excludes, final String namePattern, final String tagPattern) {
		// TODO Auto-generated method stub
		return 1;
	}
}
