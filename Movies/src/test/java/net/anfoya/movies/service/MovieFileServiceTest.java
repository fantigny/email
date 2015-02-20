package net.anfoya.movies.service;

import net.anfoya.movies.service.MovieFileService;

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
