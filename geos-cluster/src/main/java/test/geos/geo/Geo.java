package test.geos.geo;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Geo implements Comparable<Geo> {
	private static final String PRINT_FORMAT = "%d, %s, %s";
	private static final String PRINT_DATE_FORMAT = "yyyy-MM-dd";
	public static void print(PrintStream stream, Geo geo) {
		stream.format(PRINT_FORMAT
				, geo.id
				, geo.name
				, new SimpleDateFormat(PRINT_DATE_FORMAT).format(geo.date));
		stream.println();
	}

	private final long id;
	private final String name;
	private final Date date;
	public Geo(long id, String name, Date date) {
		super();
		this.id = id;
		this.name = name;
		this.date = date;
	}
	@Override
	public int compareTo(Geo g) {
		return (int) Math.signum(id - g.id);
	}
	public long getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public Date getDate() {
		return date;
	}
}
