package net.anfoya.java.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ed2kWorkaroundInputStream extends FilterInputStream {
	private static final Logger LOGGER = LoggerFactory.getLogger(Ed2kWorkaroundInputStream.class);

	public static final char REP_CHAR = '_';
	public static final String REGEX_CHAR = "" + REP_CHAR;

	private static final String ED2K_PREFIX = "ed2k://";

	private static final byte[] START_BYTES = ED2K_PREFIX.getBytes();
	private static final int START_LENGTH = START_BYTES.length;
	private static final byte[] END_BYTES = "|/".getBytes();
	private static final int END_LENGTH = END_BYTES.length;

	private int matchIndex = 0;

	private boolean inLink = false;
	private String link = "";

	public Ed2kWorkaroundInputStream(final InputStream in) {
		super(in);
	}

	@Override
	public int read() throws IOException {
		final int n = super.read();
	    return n == -1? n: 128 + encode((byte) (n - 128));
	}
    @Override
	public int read(final byte b[], final int off, final int len) throws IOException {
        final int n = super.read(b, off, len);
    	for(int i=0; i<n; i++) {
    		b[i] = encode(b[i]);
    	}
        return n;
    }

    public static String decode(final String s) {
    	return s.replaceAll(REGEX_CHAR, "|");
    }

	private byte encode(byte chr) {
	    if (inLink) {
	    	if (chr == END_BYTES[matchIndex]) {
	    		matchIndex++;
	    		if (matchIndex == END_LENGTH) {
	    			inLink = false;
	    			matchIndex = 0;
	    		}
	    	} else {
	    		matchIndex = 0;
	    	}
		    if (inLink) {
		    	if (chr == '|') {
		    		chr = REP_CHAR;
		    	}
		    	link += new String(new byte[] { chr });
		    } else {
		    	LOGGER.info("applied {}", link);
		    	link = "";
		    }
	    } else {
	    	if (chr == START_BYTES[matchIndex]) {
	    		matchIndex++;
	    		if (matchIndex == START_LENGTH) {
	    			inLink = true;
	    			matchIndex = 0;
	    		}
	    	} else {
	    		matchIndex = 0;
	    	}
	    }

    	return chr;
	}
}