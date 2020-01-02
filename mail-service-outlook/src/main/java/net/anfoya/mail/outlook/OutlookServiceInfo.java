package net.anfoya.mail.outlook;

import net.anfoya.mail.model.Contact;
import net.anfoya.mail.model.Message;
import net.anfoya.mail.model.Section;
import net.anfoya.mail.model.SimpleInfo;
import net.anfoya.mail.model.Tag;
import net.anfoya.mail.model.Thread;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.MailServiceInfo;

@SuppressWarnings("serial")
public class OutlookServiceInfo extends SimpleInfo implements MailServiceInfo {
	private static final String NAME = "Outlook";
	private static final String ICON_PATH = "/net/anfoya/mail/outlook/javafx/outlook.png";

	public OutlookServiceInfo() {
		super(NAME, ICON_PATH);
	}

	@Override
	public MailService<? extends Section, ? extends Tag, ? extends Thread, ? extends Message, ? extends Contact> getMailService(String appName) {
		throw new UnsupportedOperationException();
	}
}
