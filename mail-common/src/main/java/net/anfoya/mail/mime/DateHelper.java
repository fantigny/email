package net.anfoya.mail.mime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateHelper {
	private static final String TODAY_FORMAT = "HH:mm";
	private static final String WEEK_FORMAT = "EEE' 'd";
	private static final String MONTH_FORMAT = "MMM', 'EEE' 'd";
	private static final String DEFAULT_FORMAT = "yyyy' '" + MONTH_FORMAT;

	private final Calendar today;
	private final Calendar calendar;

	public DateHelper(final Date date) {
		today = Calendar.getInstance();
		calendar = Calendar.getInstance();
		if (date != null) {
			calendar.setTime(date);
		}
	}

	public String getPattern() {
		if (sameField(Calendar.YEAR)) {
			 if (sameField(Calendar.DAY_OF_YEAR)) {
				return TODAY_FORMAT;
			} else if (sameField(Calendar.MONTH)) {
				return WEEK_FORMAT;
			} else {
				return MONTH_FORMAT;
			}
		}

		return DEFAULT_FORMAT;
	}

	public String format() {
		return new SimpleDateFormat(getPattern()).format(calendar.getTime());
	}

	private boolean sameField(final int field) {
		return calendar.get(field) == today.get(field);
	}
}
