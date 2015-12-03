package net.anfoya.tag.javafx.scene.section;

import java.util.Set;

import net.anfoya.tag.javafx.scene.tag.TagList;
import net.anfoya.tag.javafx.scene.tag.TagListItem;
import net.anfoya.tag.service.Section;
import net.anfoya.tag.service.Tag;
import net.anfoya.tag.service.TagService;

public class SearchPane<S extends Section, T extends Tag> extends SectionPane<S, T> {

	private final TagList<S, T> tagList;

	public SearchPane(final TagService<S, T> tagService, final boolean showExcludeBox) {
		super(null, tagService, showExcludeBox);
		setText("search result");
		tagList = getTagList();
	}

	@Override
	public void refresh(final String pattern, final Set<T> includes, final Set<T> excludes, final String itemPattern) {
		tagList.refresh(pattern, includes, excludes, itemPattern);
		for(final TagListItem<T> item: tagList.getItems()) {
			item.includedProperty().set(true);
		}
	}

	@Override
	public synchronized void updateCountAsync(final int queryCount, final Set<T> tags, final Set<T> includes, final Set<T> excludes, final String itemPattern, final String tagPattern) {
		tagList.updateCount(queryCount, tags, includes, excludes, itemPattern);
	}

	@Override
	protected void init() {
	}
}
