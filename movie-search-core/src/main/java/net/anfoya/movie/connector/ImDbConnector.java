package net.anfoya.movie.connector;


public class ImDbConnector extends SimpleConnector implements MovieConnector {

	private static final String NAME = "ImDB";
	private static final String HOME_URL = "http://www.imdb.com";
	private static final String PATTERN_SEARCH = HOME_URL + "/find?ref_=nv_sr_fn&q=%s&s=all";

	public ImDbConnector() {
		super(NAME, HOME_URL, PATTERN_SEARCH);
	}
}
