package test.geos.geo;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Geo implements Comparable<Geo> {
	private static final String PRINT_FORMAT = "%d, %s, %s";
	private static final String PRINT_DATE_FORMAT = "yyyy-MM-dd";
	private final long id;
	private final String name;
	private final Date date;
	private final long x;
	private final long y;
	public Geo(long id, String name, Date date, long x, long y) {
		super();
		this.id = id;
		this.name = name;
		this.date = date;
		this.x = x;
		this.y = y;
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
	public void print(PrintStream stream) {
		stream.format(PRINT_FORMAT
				, id
				, name
				, new SimpleDateFormat(PRINT_DATE_FORMAT).format(date));
		stream.println();
	}
	public long getX() {
		return x;
	}
	public long getY() {
		return y;
	}
}
