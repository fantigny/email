package net.anfoya.java.net.filtered.easylist;

import java.io.File;


public class Config {
	private static final String TEMP_FOLDER = System.getProperty("java.io.tmpdir") + File.separatorChar;
	private static final String EASYLIST_EXCEPTIONS_FILEPATH = TEMP_FOLDER + "easylist_exceptions.bin";
	private static final String EASYLIST_EXCLUSIONS_FILEPATH = TEMP_FOLDER + "easylist_exclusions.bin";
	private static final String[] EASY_LIST_URLS = {
		"https://easylist-downloads.adblockplus.org/easylist.txt"
//		, "https://easylist-downloads.adblockplus.org/liste_fr.txt"
//		, "https://easylist-downloads.adblockplus.org/easyprivacy.txt"
//		, "https://easylist-downloads.adblockplus.org/malwaredomains_full.txt"
	};

	public String getExceptionsFilePath() {
		return EASYLIST_EXCEPTIONS_FILEPATH;
	}

	public String getExclusionsFilePath() {
		return EASYLIST_EXCLUSIONS_FILEPATH;
	}

	public String[] getUrls() {
		return EASY_LIST_URLS;
	}
}
