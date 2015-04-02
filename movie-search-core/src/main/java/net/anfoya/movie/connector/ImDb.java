package net.anfoya.movie.connector;

import java.util.ArrayList;
import java.util.List;

public class ImDb extends MovieConnectorAbstract implements MovieConnector {

	@Override
	public List<QuickSearchVo> find(final String pattern) {
		return new ArrayList<QuickSearchVo>();
	}
}
