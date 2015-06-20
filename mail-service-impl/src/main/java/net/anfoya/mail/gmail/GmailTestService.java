package net.anfoya.mail.gmail;

public class GmailTestService extends GmailService {

	@Override
	public void connect() throws GMailException {
		super.connect("test");
	}
}
