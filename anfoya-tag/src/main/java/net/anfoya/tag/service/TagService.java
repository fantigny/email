package net.anfoya.tag.service;

import java.util.Set;

import net.anfoya.tag.model.SpecialTag;

public interface TagService<S extends Section, T extends Tag> {

	public Set<S> getSections() throws TagException;
	public long getCountForSection(S section, Set<T> includes, Set<T> excludes, String itemPattern) throws TagException;

	public S addSection(String name) throws TagException;
	public void remove(S Section) throws TagException;
	public S rename(S Section, String name) throws TagException;
	public void hide(S Section) throws TagException;
	public void show(S Section) throws TagException;

	public T findTag(String name) throws TagException;
	public T getTag(String id) throws TagException;
	public Set<T> getTags(S section) throws TagException;
	public Set<T> getTags(String pattern) throws TagException;
	public long getCountForTags(Set<T> includes, Set<T> excludes, String pattern) throws TagException;

	public Set<T> getHiddenTags() throws TagException;
	public T getSpecialTag(SpecialTag specialTag) throws TagException;

	public T addTag(String name) throws TagException;
	public void remove(T tag) throws TagException;
	public T rename(T tag, String name) throws TagException;
	public void hide(T tag) throws TagException;
	public void show(T tag) throws TagException;

	public T moveToSection(T tag, S section) throws TagException;

	public void addOnUpdateTagOrSection(Runnable callback);
}
