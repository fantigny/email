package net.anfoya.movie.browser.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import net.anfoya.movie.browser.model.Profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfileService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileService.class);
	private static final String USER_NAME = System.getProperty("user.name");
	private static final String PROPERTY_FILENAME = "profile.properties";

	protected static final String FORCE_PROFILE_PROPERTY_NAME = "force_profile";

	private final Profile profile;

	public ProfileService(final String profileName) throws IOException {
		Profile profile = null;
		try {
			profile = Profile.valueOf(profileName);
		} catch(final Exception e) {
			LOGGER.error("profile not recognized: {}, profile should be one of {}", profileName, Arrays.asList(Profile.values()).toString());
		}
		if (profile == null) {
			profile = Profile.REGULAR;
		}
		this.profile = profile;

		LOGGER.info("user [{}, {}]", USER_NAME, profile.toString());
	}

	public ProfileService() throws IOException {
		Profile profile = null;
		final String forcedProfile = System.getProperty(FORCE_PROFILE_PROPERTY_NAME, "");
		if (!forcedProfile.isEmpty()) {
			try {
				profile = Profile.valueOf(forcedProfile);
			} catch(final Exception e) {
				LOGGER.error("profile not recognized: {}, profile should be one of {}", forcedProfile, Arrays.asList(Profile.values()).toString());
			}
		} else {
			final Properties properties = new Properties();
			properties.load(getClass().getResourceAsStream(PROPERTY_FILENAME));

			for(final Profile p: Profile.values()) {
				final String nameString = (String) properties.getOrDefault(p.toString(), "");
				final String[] names = nameString.split(",");
				for(final String name: names) {
					if (USER_NAME.equals(name)) {
						profile = p;
						break;
					}
				}
			}
		}

		if (profile == null) {
			profile = Profile.REGULAR;
		}

		this.profile = profile;

		LOGGER.info("user [{}, {}]", USER_NAME, profile.toString());
	}

	public Profile getProfile() {
		return profile;
	}
}