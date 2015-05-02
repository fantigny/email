package net.anfoya.javafx.scene.control.tag.service;

import java.util.Set;

import net.anfoya.javafx.scene.control.tag.model.Section;
import net.anfoya.javafx.scene.control.tag.model.Tag;

public interface TagService {

	Set<Section> getSections();
	Set<Tag> getTags(Section section, String tagPattern);
	void addToSection(Tag tag);
	int getCount(Set<Tag> includes, Set<Tag> excludes, String pattern);
	Integer getSectionCount(Section section, Set<Tag> includes, Set<Tag> excludes, String namePattern, String tagPattern);
}
