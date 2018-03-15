package net.anfoya.movie.browser.service;

import static net.anfoya.movie.browser.service.ProfileService.FORCE_PROFILE_PROPERTY_NAME;

import java.io.IOException;

import net.anfoya.movie.browser.model.Profile;

import org.junit.Assert;
import org.junit.Test;

public class ProfileServiceTest {

	@Test
	public void getProfile() throws IOException {
		final Profile profile = new ProfileService().getProfile();
		Assert.assertNotNull(profile);
	}

	@Test
	public void force() throws IOException {
		for(final Profile profileRef: Profile.values()) {
			System.setProperty(FORCE_PROFILE_PROPERTY_NAME, profileRef.toString());
			final Profile profile = new ProfileService().getProfile();
			Assert.assertEquals(profileRef, profile);
		}
	}

	@Test
	public void forceBadProfile() throws IOException {
		System.setProperty(FORCE_PROFILE_PROPERTY_NAME, "n/d");
		final Profile profile = new ProfileService().getProfile();
		Assert.assertEquals(Profile.REGULAR, profile);
	}
}
