package net.anfoya.mail.browser.javafx.settings;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.MessagingException;

import net.anfoya.mail.browser.javafx.attachment.AttachmentLoader;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;

public class MailStorage<T extends Tag, H extends Thread, M extends Message> {
	private static final String TAG = ".fishermail";
	private static final String SUBJECT = TAG + "-settings";
	private static final String FILENAME = SUBJECT + ".bin";

	private MailService<?, T, H, M, ?> mailService;

	public MailStorage(MailService<?, T, H, M, ?> mailService) {
		this.mailService = mailService;
	}
	
	public List<Object> loadFromMail() {
		try {
			T tag = mailService.getTag(TAG);
			if (tag == null) {
				tag = mailService.addTag(TAG);
			}
			Set<T> includes = new HashSet<T>();
			includes.add(tag);
			Set<H> threads = mailService.findThreads(includes, Collections.emptySet(), SUBJECT, 1000);
			if (!threads.isEmpty()) {
				AttachmentLoader<M> loader = new AttachmentLoader<>(mailService, threads.iterator().next().getLastMessageId());
				final String filename = loader.saveAttachment(FILENAME);				
			}
		} catch (MailException | IOException | MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
