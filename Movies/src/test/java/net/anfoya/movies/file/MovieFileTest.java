package net.anfoya.movies.file;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;

import net.anfoya.movies.io.MovieFile;
import net.anfoya.movies.io.MovieFileFactory;

import org.junit.Test;

public class MovieFileTest {

	@Test
	public void urlTest() throws MalformedURLException {
		MovieFileFactory.getFile("smb://usr:pwd@server/share/fol/der/to/name.ext");
		MovieFileFactory.getFile("smbfs://usr:pwd@server/share/fol/der/to/name.ext");
		MovieFileFactory.getFile("cif://usr:pwd@server/share/fol/der/to/name.ext");
		MovieFileFactory.getFile("cifs://usr:pwd@server/share/fol/der/to/name.ext");
	}

	@Test
	public void fileTest() throws MalformedURLException {
		final MovieFile file = MovieFileFactory.getFile("smb://usr:pwd@server/share/fol/der/to/name.ext");

		assertEquals("name.ext", file.getName());
		assertEquals("ext", file.getExtension());

		assertEquals("share", file.getShare());
		assertEquals("smb://usr:pwd@server/share/", file.getShareUrl());

		assertEquals("/fol/der/to/", file.getFolder());
		assertEquals("smb://usr:pwd@server/share/fol/der/to/", file.getFolderUrl());

		assertEquals("/fol/der/to/name.ext", file.getShortPath());
	}

	@Test
	public void folderTest() throws MalformedURLException {
		final MovieFile file = MovieFileFactory.getFile("smb://usr:pwd@server/share/fol/der/to/");

		assertEquals("to/", file.getName());
		assertEquals("", file.getExtension());

		assertEquals("share", file.getShare());
		assertEquals("smb://usr:pwd@server/share/", file.getShareUrl());

		assertEquals("/fol/der/to/", file.getFolder());
		assertEquals("smb://usr:pwd@server/share/fol/der/to/", file.getFolderUrl());

		assertEquals("/fol/der/to/", file.getShortPath());
	}
}
