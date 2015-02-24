package net.anfoya.java.net.filtered.easylist.model;

public class Config {
	private static final String EASYLIST_FILEPATH = System.getProperty("java.io.tmpdir") + "/easylist.bin";
	private static final String[] EASY_LIST_URLS = {
		"https://easylist-downloads.adblockplus.org/easylist.txt"
		, "https://easylist-downloads.adblockplus.org/liste_fr.txt"
	};

	public String getFilePath() {
		return EASYLIST_FILEPATH;
	}

	public String[] getUrls() {
		return EASY_LIST_URLS;
	}
}
