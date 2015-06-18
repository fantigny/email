package net.anfoya.java.net.cookie;

import java.io.File;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.anfoya.java.io.JsonFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;

@SuppressWarnings("serial")
public class PersistentCookieStore
		extends JsonFile<Map<URI, List<HttpCookie>>>
		implements CookieStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(PersistentCookieStore.class);
	private static final String COOKIE_FILEPATH = System.getProperty("java.io.tmpdir") + File.separatorChar + "cookie_store.json";

    private final CookieStore delegate;

    public PersistentCookieStore() {
    	super(COOKIE_FILEPATH);
    	this.delegate = new CookieManager().getCookieStore();

    	Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				save();
			}
		}));
    }

	public void load() {
		Map<URI, List<HttpCookie>> cookieMap = null;
    	try {
        	cookieMap = load(new TypeToken<Map<URI, List<HttpCookie>>>(){}.getType());
		} catch (final Exception e) {
			LOGGER.warn("reading {}", this, e);
		}

        if (cookieMap != null) {
        	for(final Entry<URI, List<HttpCookie>> entry: cookieMap.entrySet()) {
        		final URI uri = entry.getKey();
        		for(final HttpCookie cookie: entry.getValue()) {
            		LOGGER.debug("adding cookie {} -- {}", uri.toString(), cookie.toString());
        			delegate.add(uri, cookie);
        		}
        	}

        	LOGGER.info("loaded {} cookies", cookieMap.values().size());
        }
	}

	private void save() {
    	final Map<URI, List<HttpCookie>> cookieMap = new HashMap<URI, List<HttpCookie>>();
    	for(final URI uri: delegate.getURIs()) {
    		final List<HttpCookie> cookies = delegate.get(uri);
    		cookieMap.put(uri, cookies);
    		LOGGER.debug("adding cookie {} -- {}", uri.toString(), cookies.toString());
    	}

    	LOGGER.info("saving {} cookies", cookieMap.values().size());
        try {
        	save(cookieMap);
		} catch (final IOException e) {
			LOGGER.error("writing {}", this, e);
		}
    }

	@Override
	public void add(final URI uri, final HttpCookie cookie) {
		delegate.add(uri, cookie);
	}

	@Override
	public List<HttpCookie> get(final URI uri) {
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
	public boolean remove(final URI uri, final HttpCookie cookie) {
		return delegate.remove(uri, cookie);
	}

	@Override
	public boolean removeAll() {
		return delegate.removeAll();
	}
}
