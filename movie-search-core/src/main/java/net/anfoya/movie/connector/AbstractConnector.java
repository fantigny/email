package net.anfoya.movie.connector;

import java.util.List;

public abstract class AbstractConnector implements MovieConnector {

	@Override
	public QuickSearchVo findBestMatch(final String pattern) {
		final List<QuickSearchVo> qsVos = find(pattern);
		final QuickSearchVo bestMatch;
		if (!qsVos.isEmpty()) {
			bestMatch = qsVos.get(0);
		} else {
			bestMatch = null;
		}

		return bestMatch;
	}
}
