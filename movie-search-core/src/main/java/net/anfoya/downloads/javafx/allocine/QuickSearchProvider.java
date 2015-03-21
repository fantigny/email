package net.anfoya.downloads.javafx.allocine;

import java.util.List;

public interface QuickSearchProvider {

	List<QuickSearchVo> search(String pattern);
}
