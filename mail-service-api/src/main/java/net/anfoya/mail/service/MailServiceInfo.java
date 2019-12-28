package net.anfoya.mail.service;

import java.io.Serializable;

import javafx.scene.image.Image;
import net.anfoya.mail.model.Contact;
import net.anfoya.mail.model.Message;
import net.anfoya.mail.model.Section;
import net.anfoya.mail.model.Tag;
import net.anfoya.mail.model.Thread;

public interface MailServiceInfo extends Serializable{
	String getName();
	Image getIcon();
	MailService<? extends Section, ? extends Tag, ? extends Thread, ? extends Message, ? extends Contact> getMailService(String appName);
}
