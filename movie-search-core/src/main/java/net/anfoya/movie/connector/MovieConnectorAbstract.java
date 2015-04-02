package net.anfoya.movie.connector;

import java.util.List;

public abstract class MovieConnectorAbstract implements MovieConnector {

	@Override
	public QuickSearchVo findBestMatch(final String pattern) {
		QuickSearchVo bestMatch = null;
		final List<QuickSearchVo> qsVos = find(pattern);
		if (!qsVos.isEmpty()) {
			for(final QuickSearchVo qsVo: qsVos) {
				if (qsVo.getName().equals(pattern) || qsVo.getFrench().equals(pattern)) {
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

		return bestMatch == null? QuickSearchVo.getEmptyValue(): bestMatch;
	}
}
