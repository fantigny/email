package net.anfoya.tag.service;

import java.util.Set;

import javafx.util.Callback;

public interface TagService<S extends Section, T extends Tag> {

	public S addSection(String name) throws TagException;
	public S rename(S Section, String name) throws TagException;
	public void remove(S Section) throws TagException;
	public Set<S> getSections() throws TagException;
	public int getCountForSection(S section, Set<T> includes, Set<T> excludes, String itemPattern) throws TagException;

	public T addTag(String name) throws TagException;
	public T rename(T tag, String name) throws TagException;
	public void remove(T tag) throws TagException;
	public Set<T> getTags(S section) throws TagException;
	public Set<T> getTags(String pattern) throws TagException;
	public int getCountForTags(Set<T> includes, Set<T> excludes, String pattern) throws TagException;

	public T moveToSection(T tag, S section) throws TagException;

	public void addOnUpdateTagOrSection(Callback<Void, Void> callback);
}
