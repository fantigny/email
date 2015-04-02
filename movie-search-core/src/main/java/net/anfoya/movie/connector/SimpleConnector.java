package net.anfoya.movie.connector;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import net.anfoya.movie.connector.MovieVo.Type;

public class SimpleConnector implements MovieConnector {

	private final String name;
	private final String homeUrl;
	private final String searchPattern;

	public SimpleConnector(final String name, final String homeUrl, final String searchPattern) {
		this.name = name;
		this.homeUrl = homeUrl;
		this.searchPattern = searchPattern;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getHomeUrl() {
		return homeUrl;
	}

	@Override
	public boolean isSearchable() {
		return !searchPattern.isEmpty();
	}

	@Override
	public String getSearchUrl(final String pattern) {
		try {
			return String.format(searchPattern, URLEncoder.encode(pattern, "UTF-8"));
		} catch (final UnsupportedEncodingException e) {
			return String.format(searchPattern, pattern);
		}
	}

	@Override
	public List<MovieVo> findAll(final String pattern) {
		return new ArrayList<MovieVo>();
	}

	@Override
	public MovieVo find(final String pattern) {
		MovieVo bestMatch = null;
		final List<MovieVo> qsVos = findAll(pattern);
		if (!qsVos.isEmpty()) {
			for(final MovieVo qsVo: qsVos) {
				if (qsVo.getName().equalsIgnoreCase(pattern)
						|| qsVo.getFrench().equalsIgnoreCase(pattern)) {
					if (bestMatch == null) {
						bestMatch = qsVo;
					} else {
						if (bestMatch.getYear().compareTo(qsVo.getYear()) < 0) {
							bestMatch = qsVo;
						}
					}
				}
			}
		}
		if (bestMatch == null) {
			bestMatch = new MovieVo("", Type.UNDEFINED, pattern, "", "", "", getSearchUrl(pattern), "", "", "", "", getName());
		}

		return bestMatch;
	}
}
