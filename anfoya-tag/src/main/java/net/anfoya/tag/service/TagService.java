package net.anfoya.tag.service;

import java.util.List;
import java.util.Set;

import net.anfoya.tag.model.Section;
import net.anfoya.tag.model.Tag;

public interface TagService {

	Set<Section> getSections() throws TagServiceException;
	List<Tag> getTags(Section section, String tagPattern) throws TagServiceException;
	void addToSection(Tag tag);
	int getCount(Set<Tag> includes, Set<Tag> excludes, String pattern);
	int getSectionCount(Section section, Set<Tag> includes, Set<Tag> excludes, String namePattern, String tagPattern);
}
