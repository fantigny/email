package net.anfoya.movie.connector;


public class MovieVo {
	private static final MovieVo EMPTY = new MovieVo();

	public static MovieVo getEmptyValue() {
		return EMPTY;
	}

	public enum Type {
		MOVIE, PERSON, SERIE, UNDEFINED
	}

	private final String id;
	private final Type type;
	private final String name;
	private final String french;
	private final String year;
	private final String thumbnail;
	private final String url;

	private final String director;
	private final String activity;
	private final String creator;
	private final String country;

	private final String source;

	public MovieVo(final String id, final Type type, final String name, final String french,
			final String year, final String thumbnail, final String url, final String director, final String activity,
			final String creator, final String country, final String source) {
		super();
		this.id = id;
		this.type = type;
		this.name = name;
		this.french = french;
		this.year = year;
		this.thumbnail = thumbnail;
		this.url = url;
		this.director = director;
		this.activity = activity;
		this.creator = creator;
		this.country = country;
		this.source = source;
	}



	public MovieVo(final String name) {
		this("", Type.UNDEFINED, name, "", "", "", "", "", "", "", "", "");
	}

	private MovieVo() {
		this("");
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof MovieVo) {
			final MovieVo other = (MovieVo) o;
			if (other.name.isEmpty()) {
				return name.isEmpty();
			} else {
				return other.name.equals(name) || other.name.equals(french);
			}
		}

		return false;
	}

	@Override
	public String toString() {
		return getName();
	}

	public String getFrench() {
		return french;
	}

	public String getName() {
		return name;
	}

	public String getThumbnail() {
		return thumbnail;
	}

	public String getId() {
		return id;
	}

	public String getDirector() {
		return director;
	}

	public String getYear() {
		return year;
	}

	public String getActivity() {
		return activity;
	}

	public Type getType() {
		return type;
	}

	public String getCreator() {
		return creator;
	}

	public boolean isEmpty() {
		return equals(EMPTY);
	}

	public String getCountry() {
		return country;
	}

	public String getUrl() {
		return url;
	}

	public String getSource() {
		return source;
	}
}