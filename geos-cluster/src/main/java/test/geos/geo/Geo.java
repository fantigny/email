package test.geos.geo;

import java.io.PrintStream;
import java.util.Date;

public class Geo {
	private static final String PRINT_FORMAT = "%d, %s, %s";
	private final long id;
	private final String name;
	private final Date date;
	public Geo(long id, String name, Date date) {
		super();
		this.id = id;
		this.name = name;
		this.date = date;
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
	public void print(PrintStream stream) {
		stream.format(PRINT_FORMAT, id, name, date);
		stream.println();
	}
}
