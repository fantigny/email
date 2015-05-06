package net.anfoya.tag.service;

import java.util.Set;

import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public interface TagService<S extends SimpleSection, T extends SimpleTag> {

	public Set<S> getSections() throws TagServiceException;
	public int getCountForSection(S section, Set<T> includes, Set<T> excludes, String namePattern, String tagPattern) throws TagServiceException;
	public void moveToSection(S section, T tag) throws TagServiceException;
	public S addSection(String sectionName) throws TagServiceException;

	public Set<T> getTags() throws TagServiceException;
	public Set<T> getTags(S section, String tagPattern) throws TagServiceException;
	public int getCountForTags(Set<T> includes, Set<T> excludes, String pattern) throws TagServiceException;
}
