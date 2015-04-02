package net.anfoya.movie.connector;

import java.util.List;

public interface MovieConnector {

	public String getName();
	public boolean isSearchable();

	public String getHomeUrl();
	public String getSearchUrl(String pattern);

	public MovieVo find(String pattern);
	public List<MovieVo> suggest(String pattern);
}
