package net.anfoya.tag.service;

import java.util.Set;

import net.anfoya.tag.model.Section;
import net.anfoya.tag.model.Tag;

public interface TagService {

	public Set<Section> getSections() throws TagServiceException;
	public int getSectionCount(Section section, Set<Tag> includes, Set<Tag> excludes, String namePattern, String tagPattern) throws TagServiceException;
	public void addToSection(Section section, Tag tag);
	public Section addSection(String sectionName);

	public Set<Tag> getTags() throws TagServiceException;
	public Set<Tag> getTags(Section section, String tagPattern) throws TagServiceException;
	public int getTagCount(Set<Tag> includes, Set<Tag> excludes, String pattern) throws TagServiceException;
}
