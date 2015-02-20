package net.anfoya.tools.net;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class PersistentCookieStore implements CookieStore {
	private static final Logger LOGGER = LoggerFactory.getLogger(PersistentCookieStore.class);
	private static final String COOKIE_FILEPATH = System.getProperty("java.io.tmpdir") + "/cookie_store.json";

	private final File file;
    private final CookieStore delegate;

    public PersistentCookieStore() {
        this.file = new File(PersistentCookieStore.COOKIE_FILEPATH);
    	this.delegate = new CookieManager().getCookieStore();
    }

	public void load() {
		File file = new File(COOKIE_FILEPATH);
		if (!file.exists()) {
			return;
		}
		
        LOGGER.info("loading {}", file.getPath());
    	BufferedReader reader = null;
    	Map<URI, List<HttpCookie>> cookieMap = null;
    	try {
    		reader = new BufferedReader(new FileReader(file));
    		cookieMap = new Gson().fromJson(reader.readLine(), new TypeToken<Map<URI, List<HttpCookie>>>(){}.getType());
		} catch (final Exception e) {
			LOGGER.warn("reading {}", file.getPath(), e);
		} finally {
			try {
				reader.close();
			} catch (final Exception e) {}
		}

        if (cookieMap != null) {
        	for(final Entry<URI, List<HttpCookie>> entry: cookieMap.entrySet()) {
        		final URI uri = entry.getKey();
        		for(final HttpCookie cookie: entry.getValue()) {
            		LOGGER.debug("adding cookie {} -- {}", uri.toString(), cookie.toString());
        			delegate.add(uri, cookie);
        		}
        	}
        }
	}

	public void save() {
    	LOGGER.info("saving to {}", file.getPath());

    	final Map<URI, List<HttpCookie>> cookieMap = new HashMap<URI, List<HttpCookie>>();
    	for(final URI uri: delegate.getURIs()) {
    		final List<HttpCookie> cookies = delegate.get(uri);
    		cookieMap.put(uri, cookies);
    		LOGGER.debug("adding cookie {} -- {}", uri.toString(), cookies.toString());
    	}

    	BufferedWriter writer = null;
        try {
        	writer = new BufferedWriter(new FileWriter(file));
        	writer.write(new Gson().toJson(cookieMap));
		} catch (final IOException e) {
			LOGGER.error("writing {}", file.getPath(), e);
		} finally {
			try {
				writer.close();
			} catch (final IOException e) {}
		}
    }
	
	@Override
	public void add(URI uri, HttpCookie cookie) {
		delegate.add(uri, cookie);
	}

	@Override
	public List<HttpCookie> get(URI uri) {
		return delegate.get(uri);
	}

	@Override
	public List<HttpCookie> getCookies() {
		return delegate.getCookies();
	}

	@Override
	public List<URI> getURIs() {
		return delegate.getURIs();
	}

	@Override
	public boolean remove(URI uri, HttpCookie cookie) {
		return delegate.remove(uri, cookie);
	}

	@Override
	public boolean removeAll() {
		return delegate.removeAll();
	}
}
