package net.anfoya.movies.file;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFileFilter;

public interface MovieFile {

	public List<MovieFile> listSmbFiles(SmbFileFilter filter) throws SmbException;
	public List<MovieFile> getListRec(SmbFileFilter filter) throws SmbException, MalformedURLException;
	public List<MovieFile> getListRec(int depth, SmbFileFilter filter) throws SmbException, MalformedURLException;

	public void open() throws IOException;
	public void showInFileExplorer() throws IOException;

	public String getName();
	public String getExtension();

	public long getLastModified();

	public URL getURL();

	public String getShare();
	public String getShareUrl();

	public String getFolder();
	public String getFolderUrl();

	public String getShortPath();

	public void delete() throws SmbException;
	public void renameTo(MovieFile target) throws SmbException;
	public boolean exists() throws SmbException;
	public boolean isDirectory() throws SmbException;
}
