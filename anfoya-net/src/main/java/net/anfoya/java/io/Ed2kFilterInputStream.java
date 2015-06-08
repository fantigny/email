package net.anfoya.java.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Ed2kFilterInputStream extends FilterInputStream {
	private static final byte[] ED2K_START_BYTES = "ed2k://".getBytes();
	private static final int ED2K_START_LENGTH = ED2K_START_BYTES.length;
	private static final byte[] ED2K_END_BYTES = "|/".getBytes();
	private static final int ED2K_END_LENGTH = ED2K_END_BYTES.length;

	private boolean ed2kLink = false;
	private int matchIndex = 0;

	public Ed2kFilterInputStream(final InputStream in) {
		super(in);
	}

	@Override
	public int read() throws IOException {
	    return fix((byte) super.read());
	}
    @Override
	public int read(final byte b[], final int off, final int len) throws IOException {
        final int n = in.read(b, off, len);
    	for(int i=0; i<n; i++) {
    		b[i] = fix(b[i]);
    	}
        return n;
    }

	private byte fix(final byte chr) {
	    if (ed2kLink) {
	    	if (chr == ED2K_END_BYTES[matchIndex]) {
	    		matchIndex++;
	    		if (matchIndex == ED2K_END_LENGTH) {
	    			ed2kLink = false;
	    			matchIndex = 0;
	    		}
	    	} else {
	    		matchIndex = 0;
	    	}
		    if (ed2kLink && chr == '|') {
		    	return '-';
		    }
	    } else {
	    	if (chr == ED2K_START_BYTES[matchIndex]) {
	    		matchIndex++;
	    		if (matchIndex == ED2K_START_LENGTH) {
	    			ed2kLink = true;
	    			matchIndex = 0;
	    		}
	    	} else {
	    		matchIndex = 0;
	    	}
	    }

    	return chr;
	}
}