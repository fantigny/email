package net.anfoya.movies.service;

import static net.anfoya.movies.service.ProfileService.FORCE_PROFILE_PROPERTY_NAME;

import java.io.IOException;

import net.anfoya.movies.model.Profile;
import net.anfoya.movies.service.ProfileService;

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
