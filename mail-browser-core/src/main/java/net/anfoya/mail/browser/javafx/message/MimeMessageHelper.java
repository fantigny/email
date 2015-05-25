package net.anfoya.mail.browser.javafx.message;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.util.BASE64DecoderStream;

public class MimeMessageHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(MimeMessageHelper.class);
	private static final String TEMP = System.getProperty("java.io.tmpdir") + "/";
	private static final String ATTACH_ICON_PATH = TEMP + "/fishermail-attachment.png";

	private static boolean copied = false;
	{
		if (!copied) {
			copied = true;
			try {
				Files.copy(getClass().getResourceAsStream("attachment.png"), new File(ATTACH_ICON_PATH).toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private final Map<String, String> cidFilenames;

	public MimeMessageHelper() {
		cidFilenames = new HashMap<String, String>();
	}

	public String toHtml(final MimeMessage message) throws IOException, MessagingException {
		cidFilenames.clear();
		String html = toHtml(message, false).toString();
		for(final Entry<String, String> entry: cidFilenames.entrySet()) {
			html = html.replaceAll("cid:" + entry.getKey(), "file://" + entry.getValue());
		}
		return html;
	}

	private StringBuilder toHtml(final Part part, boolean isHtml) throws IOException, MessagingException {
		final String type = part.getContentType().replaceAll("\\r", "").replaceAll("\\n", "").replaceAll("\\t", " ");
		isHtml = isHtml || type.contains("multipart/alternative");
		if (part.getContent() instanceof String && type.contains("text/html")) {
			LOGGER.debug("++++ type {}", type);
			return new StringBuilder((String) part.getContent());
		} else if (part.getContent() instanceof String && type.contains("text/plain") && !isHtml) {
			LOGGER.debug("++++ type {}", type);
			return new StringBuilder("<pre>").append(part.getContent()).append("</pre>");
		} else if (part instanceof Multipart || type.contains("multipart")) {
			LOGGER.debug("++++ type {}", type);
			final Multipart parts = (Multipart) part.getContent();
			final StringBuilder html = new StringBuilder();
			for(int i=0, n=parts.getCount(); i<n; i++) {
				html.append(toHtml(parts.getBodyPart(i), isHtml));
			}
			return html;
		} else if (part instanceof MimeBodyPart
				&& part.getContent() instanceof BASE64DecoderStream
				&& ((MimeBodyPart)part).getContentID() != null) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			final String cid = bodyPart.getContentID().replaceAll("<", "").replaceAll(">", "");
			final String tempFilename = TEMP + (part.getFileName() == null? cid: MimeUtility.decodeText(bodyPart.getFileName()));
			LOGGER.debug("++++ save {}", tempFilename);
			bodyPart.saveFile(tempFilename);
			cidFilenames.put(cid, tempFilename);
			return new StringBuilder();
		} else if (part instanceof MimeBodyPart
				&& part.getContent() instanceof BASE64DecoderStream
				&& MimeBodyPart.INLINE.equalsIgnoreCase(part.getDisposition())) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			final String tempFilename = TEMP + MimeUtility.decodeText(bodyPart.getFileName());
			LOGGER.debug("++++ save {}", tempFilename);
			bodyPart.saveFile(tempFilename);
			return new StringBuilder("<img src='file://").append(tempFilename).append("'>");
		} else {
			LOGGER.warn("---- type {}", type);
			return new StringBuilder();
		}
	}

	public String toAttachments(final Part part, boolean isHtml) throws IOException, MessagingException {
		final String type = part.getContentType().replaceAll("\\r", "").replaceAll("\\n", "").replaceAll("\\t", " ");
		isHtml = isHtml || type.contains("multipart/alternative");
		if (part.getContent() instanceof String && type.contains("text/html")) {
			LOGGER.debug("++++ type {}", type);
			return (String) part.getContent();
		} else if (part.getContent() instanceof String && type.contains("text/plain") && !isHtml) {
			LOGGER.debug("++++ type {}", type);
			return "<pre>" + part.getContent() + "</pre>";
		} else if (part instanceof Multipart || type.contains("multipart")) {
			LOGGER.debug("++++ type {}", type);
			final Multipart parts = (Multipart) part.getContent();
			final StringBuilder html = new StringBuilder();
			for(int i=0, n=parts.getCount(); i<n; i++) {
				html.append(toHtml(parts.getBodyPart(i), isHtml));
			}
			return html.toString();
		} else if (part instanceof MimeBodyPart
				&& part.getContent() instanceof BASE64DecoderStream
				&& ((MimeBodyPart)part).getContentID() != null) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			final String cid = bodyPart.getContentID().replaceAll("<", "").replaceAll(">", "");
			final String tempFilename = TEMP + (part.getFileName() == null? cid: MimeUtility.decodeText(bodyPart.getFileName()));
			LOGGER.debug("++++ save {}", tempFilename);
			bodyPart.saveFile(tempFilename);
			cidFilenames.put(cid, tempFilename);
			return "";
		} else if (part instanceof MimeBodyPart
				&& part.getContent() instanceof BASE64DecoderStream
				&& part.getDisposition() != null) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			final String tempFilename = TEMP + MimeUtility.decodeText(bodyPart.getFileName());
			LOGGER.debug("++++ save {}", tempFilename);
			bodyPart.saveFile(tempFilename);
			if (MimeBodyPart.INLINE.equalsIgnoreCase(part.getDisposition())) {
				return "<img src='file://" + tempFilename + "'>";
			} else {
				return "<a href ='file://" + tempFilename + "'><table><tr><td><img src='file://" + ATTACH_ICON_PATH + "'></td></tr><tr><td>" + MimeUtility.decodeText(bodyPart.getFileName()) + "</td></tr></table></a>";
			}
		} else {
			LOGGER.warn("---- type {}", type);
			return "";
		}
	}
}
