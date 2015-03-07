package net.anfoya.io;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;

import net.anfoya.io.SmbFileExt;
import net.anfoya.io.SmbFileExtFactory;

import org.junit.Test;

public class MovieFileTest {

	@Test
	public void urlTest() throws MalformedURLException {
		SmbFileExtFactory.getFile("smb://usr:pwd@server/share/fol/der/to/name.ext");
		SmbFileExtFactory.getFile("smbfs://usr:pwd@server/share/fol/der/to/name.ext");
		SmbFileExtFactory.getFile("cif://usr:pwd@server/share/fol/der/to/name.ext");
		SmbFileExtFactory.getFile("cifs://usr:pwd@server/share/fol/der/to/name.ext");
	}

	@Test
	public void fileTest() throws MalformedURLException {
		final SmbFileExt file = SmbFileExtFactory.getFile("smb://usr:pwd@server/share/fol/der/to/name.ext");

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
		final SmbFileExt file = SmbFileExtFactory.getFile("smb://usr:pwd@server/share/fol/der/to/");

		assertEquals("to/", file.getName());
		assertEquals("", file.getExtension());

		assertEquals("share", file.getShare());
		assertEquals("smb://usr:pwd@server/share/", file.getShareUrl());

		assertEquals("/fol/der/to/", file.getFolder());
		assertEquals("smb://usr:pwd@server/share/fol/der/to/", file.getFolderUrl());

		assertEquals("/fol/der/to/", file.getShortPath());
	}
}
