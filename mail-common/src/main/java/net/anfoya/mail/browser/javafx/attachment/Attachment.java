package net.anfoya.mail.browser.javafx.attachment;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xml.internal.messaging.saaj.packaging.mime.util.BASE64DecoderStream;

import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;

public class Attachment<M extends Message> {
	private static final Logger LOGGER = LoggerFactory.getLogger(Attachment.class);
	private static final String TEMP = System.getProperty("java.io.tmpdir") + File.separatorChar;
	private static final String DOWNLOADS = System.getProperty("user.home") + File.separatorChar + "Downloads" + File.separatorChar;

	private final MailService<? extends Section, ? extends Tag, ? extends Thread, M, ? extends Contact> mailService;
	private final String messageId;
	private final String destinationFolder;

	public Attachment(final MailService<? extends Section, ? extends Tag, ? extends Thread, M, ? extends Contact> mailService
			, final String messageId) {
		this.mailService = mailService;
		this.messageId = messageId;

		destinationFolder = new File(DOWNLOADS).exists()? DOWNLOADS: TEMP;
	}

	public void start(final String filename) throws MailException, UnsupportedEncodingException, IOException, MessagingException {
		final String filepath = save(filename);

		LOGGER.info("start {}", filepath);
		try {
			Desktop.getDesktop().open(new File(filepath));
		} catch (final IOException e) {
			LOGGER.error("start {}", filepath, e);
		}
	}

	public String save(final String name) throws MailException, UnsupportedEncodingException, IOException, MessagingException {
		LOGGER.info("download attachment {} for message {}", name, messageId);
		final M message = mailService.getMessage(messageId);
		return save(message.getMimeMessage(), name);
	}

	private String save(final Part part, final String name) throws UnsupportedEncodingException, IOException, MessagingException {
		final String type = part.getContentType()
				.toLowerCase()
				.replaceAll("[\\r,\\n]", "")
				.replaceAll("\\t", " ");
		if (part instanceof Multipart || type.contains("multipart")) {
			LOGGER.debug("++++ type {}", type);
			final Multipart parts = (Multipart) part.getContent();
			String path = "";
			for(int i=0, n=parts.getCount(); i<n; i++) {
				path += save(parts.getBodyPart(i), name);
			}
			return path;
		} else if (part instanceof MimeBodyPart
				&& part.getContent() instanceof BASE64DecoderStream
				&& part.getDisposition() == null || MimeBodyPart.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			final String filename = MimeUtility.decodeText(bodyPart.getFileName());
			if (name.equals(filename)) {
				final String path = buildPath(filename);
				LOGGER.debug("++++ save {}", path);
				bodyPart.saveFile(path);
				return path;
			} else {
				return "";
			}
		} else {
			return "";
		}
	}

	private String buildPath(String filename) {
		String path = destinationFolder + filename;
		int fileIndex = 1;
		while (new File(path).exists()) {
			if (filename.contains(".")) {
				path = destinationFolder + filename.replace(".", "_" + fileIndex + ".");
			} else {
				path = destinationFolder + filename + "_" + fileIndex;
			}
			fileIndex++;
		}
		return path;
	}
}
