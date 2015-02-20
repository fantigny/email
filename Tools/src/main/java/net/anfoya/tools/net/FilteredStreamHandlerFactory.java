package net.anfoya.tools.net;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class FilteredStreamHandlerFactory implements URLStreamHandlerFactory {
	private final UrlFilter filter;

	public FilteredStreamHandlerFactory(final UrlFilter filter) {
		this.filter = filter;
	}

	@Override
	public URLStreamHandler createURLStreamHandler(final String protocol) {
	     if ("http".equals(protocol)) {
	         return new FilteredHttpHandler(filter);
	     } else if ("https".equals(protocol)) {
	    	 return new FilteredHttpsHandler(filter);
	     } else {
	    	 return null;
	     }
	}
}
