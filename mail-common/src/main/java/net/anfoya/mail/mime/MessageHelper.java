package net.anfoya.mail.mime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	private static final String ATTACH_ICON_PATH = TEMP + "fishermail-attachment.png";

	private static boolean copied = false;
	/* prepare attachment icon */ {
		if (!copied) {
			copied = true;
			try {
				Files.copy(getClass().getResourceAsStream("attachment.png"), new File(ATTACH_ICON_PATH).toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (final IOException e) {
				LOGGER.error("can't copy attachment icon");
			}
		}
	}

	private final Map<String, String> cidFilenames;
	private final List<String> attachments;

	public MessageHelper() {
		cidFilenames = new HashMap<String, String>();
		attachments = new ArrayList<String>();
	}

	public String[] getMailAddresses(final Address[] addresses) {
		final List<String> list = new ArrayList<String>();
		if (addresses != null) {
			for(final Address address: addresses) {
				if (address.getType().equalsIgnoreCase("rfc822")) {
					final InternetAddress mailAddress = (InternetAddress) address;
					list.add(mailAddress.getAddress());
				}
			}
		}

		return list.toArray(new String[list.size()]);
	}

	public String toHtml(final MimeMessage message) throws IOException, MessagingException {
		cidFilenames.clear();
		String html = toHtml(message, false).toString();
		html = replaceCids(html, cidFilenames);
		html = addAttachments(html, attachments);
		return html;
	}

	private String addAttachments(final String html, final List<String> attachNames) {
		if (attachNames.isEmpty()) {
			return html;
		}

		String attHtml = "";
		attHtml += "<br>";
		attHtml += "<hr>";
		attHtml += "<br>";
		attHtml += "<div style='position: absolute; left: 10; bottom: 10;'>";
		attHtml += "<table cellspacing='5'><tr>";
		for(final String name: attachNames) {
			attHtml += "<td style='cursor: hand' align='center' onClick='attLoader.start(\"" + name + "\")'><img src='file://" + ATTACH_ICON_PATH + "'></td>";
		}
		attHtml += "</tr><tr>";
		for(final String name: attachNames) {
			attHtml += "<td style='font-size:12px' align='center' onClick='attLoader.start(\"" + name + "\")'>" + name + "</td>";
		}
		attHtml += "</tr></table>";
		attHtml += "</div>";
		LOGGER.debug(attHtml);

		final String start, end;
		final int pos = Math.max(html.lastIndexOf("</body>"), html.lastIndexOf("</BODY>"));
		if (pos == -1) {
			start = html;
			end = "";
		} else {
			start = html.substring(0, pos);
			end = html.substring(pos);
		}

		return start + attHtml + end;
	}

	private String replaceCids(String html, final Map<String, String> cidFilenames) {
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
		} else if (part instanceof MimeBodyPart
				&& part.getContent() instanceof BASE64DecoderStream
				&& part.getDisposition() == null || MimeBodyPart.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			final String filename = MimeUtility.decodeText(bodyPart.getFileName());
			LOGGER.debug("++++ keep {}", filename);
			attachments.add(filename);
			return new StringBuilder();
		} else {
			LOGGER.warn("---- type {}", type);
			return new StringBuilder();
		}
	}
}
