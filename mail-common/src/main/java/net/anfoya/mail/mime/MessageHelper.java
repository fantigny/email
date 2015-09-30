package net.anfoya.mail.mime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.util.BASE64DecoderStream;

public class MessageHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageHelper.class);
	private static final String TEMP = System.getProperty("java.io.tmpdir") + File.separatorChar;

	private final Map<String, String> cidFilenames;
	private final Set<String> attachmentNames;

	public MessageHelper() {
		cidFilenames = new HashMap<String, String>();
		attachmentNames = new LinkedHashSet<String>();
	}

	public static String[] getMailAddresses(final Address[] addresses) {
		final List<String> mailAddresses = new ArrayList<String>();
		if (addresses != null) {
			for(final Address address: addresses) {
				if (address.getType().equalsIgnoreCase("rfc822")) {
					final InternetAddress mailAddress = (InternetAddress) address;
					mailAddresses.add(mailAddress.getAddress());
				}
			}
		}

		return mailAddresses.toArray(new String[mailAddresses.size()]);
	}

	public static String[] getNames(final Address[] addresses) {
		final List<String> names = new ArrayList<String>();
		if (addresses != null) {
			for(final Address address: addresses) {
				if (address.getType().equalsIgnoreCase("rfc822")) {
					final InternetAddress mailAddress = (InternetAddress) address;
					if (mailAddress.getPersonal() != null) {
						names.add(mailAddress.getPersonal());
					} else {
						names.add(mailAddress.getAddress());
					}
				}
			}
		}

		return names.toArray(new String[names.size()]);
	}

	public String toHtml(final MimeMessage message) throws IOException, MessagingException {
		cidFilenames.clear();
		String html = toHtml(message, false).toString();
		html = replaceCids(html, cidFilenames);
		return html;
	}

	private String replaceCids(String html, final Map<String, String> cidFilenames) {
		for(final Entry<String, String> entry: cidFilenames.entrySet()) {
			html = html.replaceAll("cid:" + entry.getKey(), "file://" + entry.getValue());
		}
		return html;
	}

	private StringBuilder toHtml(final Part part, boolean isHtml) throws IOException, MessagingException {
		final String type = part.getContentType().replaceAll("\\r", "").replaceAll("\\n", "").replaceAll("\\t", " ");
		final Object content = part.getContent();
		isHtml = isHtml || type.contains("multipart/alternative");
		if (content instanceof String && type.contains("text/html")) {
			LOGGER.debug("++++ type {}", type);
			return new StringBuilder((String) content);
		} else if (content instanceof String && type.contains("text/plain") && !isHtml) {
			LOGGER.debug("++++ type {}", type);
			return new StringBuilder("<pre>").append(content).append("</pre>");
		} else if (part instanceof Multipart || type.contains("multipart")) {
			LOGGER.debug("++++ type {}", type);
			final Multipart parts = (Multipart) content;
			final StringBuilder html = new StringBuilder();
			for(int i=0, n=parts.getCount(); i<n; i++) {
				html.append(toHtml(parts.getBodyPart(i), isHtml));
			}
			return html;
		} else if (part instanceof MimeBodyPart
				&& content instanceof BASE64DecoderStream
				&& ((MimeBodyPart)part).getContentID() != null) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			final String cid = bodyPart.getContentID().replaceAll("<", "").replaceAll(">", "");
			final String tempFilename = TEMP + (part.getFileName() == null? cid: MimeUtility.decodeText(bodyPart.getFileName()));
			LOGGER.debug("++++ save {}", tempFilename);
			bodyPart.saveFile(tempFilename);
			cidFilenames.put(cid, tempFilename);
			return new StringBuilder();
		} else if (part instanceof MimeBodyPart
				&& content instanceof BASE64DecoderStream
				&& MimeBodyPart.INLINE.equalsIgnoreCase(part.getDisposition())) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			final String tempFilename = TEMP + MimeUtility.decodeText(bodyPart.getFileName());
			LOGGER.debug("++++ save {}", tempFilename);
			bodyPart.saveFile(tempFilename);
			return new StringBuilder("<img src='file://").append(tempFilename).append("'>");
		} else if (part instanceof MimeBodyPart
				&& content instanceof BASE64DecoderStream
				&& part.getDisposition() == null || MimeBodyPart.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			final String filename = MimeUtility.decodeText(bodyPart.getFileName());
			LOGGER.debug("++++ keep {}", filename);
			attachmentNames.add(filename);
			return new StringBuilder();
		} else {
			LOGGER.warn("---- type {}", type);
			return new StringBuilder();
		}
	}

	public Set<String> getAttachmentNames() {
		return Collections.unmodifiableSet(attachmentNames);
	}
}
