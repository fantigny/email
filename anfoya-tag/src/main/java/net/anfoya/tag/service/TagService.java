package net.anfoya.tag.service;

import java.util.Set;

import net.anfoya.tag.model.Section;
import net.anfoya.tag.model.Tag;

public interface TagService<S extends Section, T extends Tag> {

	public Set<S> getSections() throws TagServiceException;
	public int getSectionCount(S section, Set<T> includes, Set<T> excludes, String namePattern, String tagPattern) throws TagServiceException;
	public void moveToSection(S section, T tag) throws TagServiceException;
	public S addSection(String sectionName) throws TagServiceException;

	public Set<T> getTags() throws TagServiceException;
	public Set<T> getTags(S section, String tagPattern) throws TagServiceException;
	public int getTagCount(Set<T> includes, Set<T> excludes, String pattern) throws TagServiceException;
}
