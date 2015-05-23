package net.anfoya.tag.javafx.scene.dnd;

import javafx.scene.input.DataFormat;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public class DndFormat {
	public static final DataFormat SECTION_DATA_FORMAT = new DataFormat(SimpleSection.class.getCanonicalName());
	public static final DataFormat TAG_DATA_FORMAT = new DataFormat(SimpleTag.class.getCanonicalName());
	public static final DataFormat TAG_NAME_DATA_FORMAT = new DataFormat("tag-name-" + String.class.getCanonicalName());
}
