package net.anfoya.movie.connector;

import java.util.List;

public interface MovieConnector {

	List<QuickSearchVo> find(String pattern);
	QuickSearchVo findBestMatch(String pattern);
}
