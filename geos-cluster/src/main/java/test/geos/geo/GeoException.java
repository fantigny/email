package test.geos.geo;

@SuppressWarnings("serial")
public class GeoException extends Exception {

	public GeoException(String msg, Throwable t) {
		super(msg, t);
	}

	public GeoException(String msg) {
		super(msg);
	}

}
