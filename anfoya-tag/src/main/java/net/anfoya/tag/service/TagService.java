package net.anfoya.tag.service;

import java.util.Set;

import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public interface TagService<S extends SimpleSection, T extends SimpleTag> {

	public S addSection(String name) throws TagException;
	public S rename(S Section, String name) throws TagException;
	public void remove(S Section) throws TagException;

	public Set<S> getSections() throws TagException;
	public int getCountForSection(S section, Set<T> includes, Set<T> excludes, String namePattern, String tagPattern) throws TagException;
	public T moveToSection(T tag, S section) throws TagException;

	public T addTag(String name) throws TagException;
	public T rename(T tag, String name) throws TagException;
	public void remove(T tag) throws TagException;

	public Set<T> getTags() throws TagException;
	public Set<T> getTags(S section, String tagPattern) throws TagException;
	public int getCountForTags(Set<T> includes, Set<T> excludes, String pattern) throws TagException;
}
