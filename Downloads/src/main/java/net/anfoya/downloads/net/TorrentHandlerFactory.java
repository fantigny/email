package net.anfoya.downloads.net;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import net.anfoya.tools.net.UrlFilter;

public class TorrentHandlerFactory implements URLStreamHandlerFactory {
	private final UrlFilter filter;

	public TorrentHandlerFactory(final UrlFilter filter) {
		this.filter = filter;
	}

	@Override
	public URLStreamHandler createURLStreamHandler(final String protocol) {
		if ("magnet".equals(protocol)){
	    	 return new MagnetHandler();
	     } else if ("http".equals(protocol)) {
	         return new TorrentHttpHandler(filter);
	     } else if ("https".equals(protocol)) {
	         return new TorrentHttpsHandler(filter);
	     } else {
	    	 return null;
	     }
	}
}
