package net.anfoya.java.net.url;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.anfoya.java.net.url.filter.Matcher;
import net.anfoya.java.net.url.handler.FilteredHttpHandler;
import net.anfoya.java.net.url.handler.FilteredHttpsHandler;
import net.anfoya.java.net.url.handler.StartHandler;

public class CustomHandlerFactory implements URLStreamHandlerFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomHandlerFactory.class);
    private static final String protocolPathProp = "java.protocol.handler.pkgs";
	private final Matcher matcher;

	public CustomHandlerFactory(final Matcher matcher) {
		this.matcher = matcher;
	}

	@Override
	public URLStreamHandler createURLStreamHandler(final String protocol) {
		if ("ed2k".equals(protocol)) {
			return new StartHandler();
		} else if ("magnet".equals(protocol)) {
			return new StartHandler();
		} else if ("http".equals(protocol)) {
			return new FilteredHttpHandler(matcher);
		} else if ("https".equals(protocol)) {
			return new FilteredHttpsHandler(matcher);
		}
		return getJavaHandler(protocol);
	}

	private URLStreamHandler getJavaHandler(final String protocol) {
		if ("data".equals(protocol)
				|| "about".equals(protocol)) {
			return null;
		}

		String packagePrefixList = null;

		packagePrefixList = java.security.AccessController.doPrivileged(
		    new sun.security.action.GetPropertyAction(protocolPathProp,""));
		if (packagePrefixList != "") {
		    packagePrefixList += "|";
		}

		// REMIND: decide whether to allow the "null" class prefix
		// or not.
		packagePrefixList += "sun.net.www.protocol";

		final StringTokenizer packagePrefixIter = new StringTokenizer(packagePrefixList, "|");

		URLStreamHandler handler = null;
		while (handler == null &&
		       packagePrefixIter.hasMoreTokens()) {

		    final String packagePrefix =
		      packagePrefixIter.nextToken().trim();
		    try {
		        final String clsName = packagePrefix + "." + protocol + ".Handler";
		        Class<?> cls = null;
		        try {
		            cls = Class.forName(clsName);
		        } catch (final ClassNotFoundException e) {
		            final ClassLoader cl = ClassLoader.getSystemClassLoader();
		            if (cl != null) {
		                cls = cl.loadClass(clsName);
		            }
		        }
		        if (cls != null) {
		            handler = (URLStreamHandler) cls.getConstructor().newInstance();
		        }
		    } catch (final Exception e) {
		        // any number of exceptions can get thrown here
		    }
		}
		if (handler == null) {
			LOGGER.warn("no handler for {}", protocol);
		}
		return handler;
	}
}
