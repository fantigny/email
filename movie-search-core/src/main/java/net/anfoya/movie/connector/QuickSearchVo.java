package net.anfoya.movie.connector;


public class QuickSearchVo {
	private static final QuickSearchVo EMPTY_QS_VO = new QuickSearchVo();

	public static QuickSearchVo getEmptyValue() {
		return EMPTY_QS_VO;
	}

	public enum Type {
		PERSON, SERIE, MOVIE, UNDEFINED
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

	public QuickSearchVo(final String id, final Type type, final String name, final String french,
			final String year, final String thumbnail, final String url, final String director, final String activity,
			final String creator, final String country) {
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
	}



	public QuickSearchVo(final String name) {
		this("", Type.UNDEFINED, name, "", "", "", "", "", "", "", "");
	}

	private QuickSearchVo() {
		this("");
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof QuickSearchVo) {
			final QuickSearchVo other = (QuickSearchVo) o;
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
		return equals(EMPTY_QS_VO);
	}

	public String getCountry() {
		return country;
	}

	public String getUrl() {
		return url;
	}
}