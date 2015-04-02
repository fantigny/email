package net.anfoya.movie.browser.service;

import net.anfoya.movie.browser.service.MovieFileService;

import org.junit.BeforeClass;
import org.junit.Test;

public class MovieFileServiceTest {

	@BeforeClass
	public static void init() {

	}

	@Test
	public void create() {
		new MovieFileService();
	}
}
