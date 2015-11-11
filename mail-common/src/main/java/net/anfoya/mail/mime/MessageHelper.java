package net.anfoya.mail.mime;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.swing.Icon;
import javax.swing.JFileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.util.BASE64DecoderStream;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import sun.misc.BASE64Encoder;
import sun.misc.IOUtils;

public class MessageHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageHelper.class);
	private static final String TEMP = System.getProperty("java.io.tmpdir") + File.separatorChar;
	private static final String SYS_ICON = "/net/anfoya/mail/sysicon/%s.png";

	private final Map<String, String> cidUris;
	private final Set<String> attachmentNames;

	public MessageHelper() {
		cidUris = new HashMap<String, String>();
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
		cidUris.clear();
		String html = toHtml(message, false).toString();
		html = replaceCids(html, cidUris);
		LOGGER.debug("{}", html);
		return html;
	}

	private String replaceCids(String html, final Map<String, String> cidFilenames) {
		for(final Entry<String, String> entry: cidFilenames.entrySet()) {
			final String cid = Matcher.quoteReplacement("cid:" + entry.getKey());
			final String uri = Matcher.quoteReplacement(entry.getValue());
			html = html.replaceAll(cid, uri);
		}
		return html;
	}

	private StringBuilder toHtml(final Part part, boolean isHtml) throws IOException, MessagingException {
		final Object content = part.getContent();
		final String type = part.getContentType()
				.toLowerCase()
				.replaceAll("[\\r,\\n]", "")
				.replaceAll("\\t", " ");
		isHtml = isHtml || type.contains("multipart/alternative");
		LOGGER.info("isHtml({}) type({}) part/content({}/{})", isHtml, type, part.getClass(), content.getClass());
		if (part instanceof Multipart || type.contains("multipart")) {
			final Multipart parts = (Multipart) content;
			final StringBuilder html = new StringBuilder();
			for(int i=0, n=parts.getCount(); i<n; i++) {
				html.append(toHtml(parts.getBodyPart(i), isHtml));
			}
			return html;
		}
		if (content instanceof String) {
			if (type.contains("text/html")) {
				return new StringBuilder((String) content);
			} else if (type.contains("text/plain")) {
				if (isHtml) {
					LOGGER.info("discarding plain text part");
					return new StringBuilder();
				}
				return new StringBuilder("<pre>").append(content).append("</pre>");
			}
		}
		if (part instanceof MimeBodyPart && content instanceof BASE64DecoderStream) {
			if (type.contains("text/calendar")) {
				try {
					new CalendarBuilder().build((InputStream) content);
					//TODO: render calendar
				} catch (final ParserException e) {
					LOGGER.error("parsing ICS", e);
				}
				return new StringBuilder();
			}
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			if (bodyPart.getContentID() != null
					&& !MimeBodyPart.ATTACHMENT.equals(bodyPart.getDisposition())) {
				final String cid = bodyPart.getContentID().replaceAll("[<,>]", "");
				final File file = new File(TEMP + cid);
				LOGGER.info("saving cid {}", file);
				bodyPart.saveFile(file);
				cidUris.put(cid, file.toURI().toString());
				return new StringBuilder();
			}
			if (bodyPart.getFileName() != null) {
				final String filename = MimeUtility.decodeText(bodyPart.getFileName());
				if (MimeBodyPart.INLINE.equals(bodyPart.getDisposition())) {
					final File file = new File(TEMP + filename);
					LOGGER.info("saving inline file {}", file);
					bodyPart.saveFile(file);
					if (type.contains("image/")) {
						return new StringBuilder("<img src='").append(file.toURI()).append("'>");
					} else {
						return new StringBuilder()
								.append("<a href='").append(file.toURI()).append("'>")
									.append("<img width='32' height='32' title='").append(filename).append("' ")
									.append("src='data:image/png;base64,").append(getBase64SystemIcon(file)).append("'>")
								.append("</a>");
					}
				}
				if (MimeBodyPart.ATTACHMENT.equals(bodyPart.getDisposition())) {
					LOGGER.info("saving reference to attachment {}", filename);
					attachmentNames.add(filename);
					return new StringBuilder();
				}
			}
		}

		LOGGER.warn("not handled {}/{}", type, content.getClass());
		return new StringBuilder();
	}

	private String getBase64SystemIcon(File file) {
		try {
			final byte[] imgBytes;
			final String iconFileName = String.format(SYS_ICON, file.getName().substring(file.getName().lastIndexOf(".") + 1));
			if (getClass().getResource(iconFileName) != null) {
				final InputStream in = getClass().getResourceAsStream(iconFileName);
				imgBytes = IOUtils.readFully(in, in.available(), true);
			} else {
				final Icon icon = new JFileChooser().getIcon(file);
				final BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TRANSLUCENT);
		        icon.paintIcon(null, bi.createGraphics(), 0, 0);
		        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ImageIO.write(bi, "png", bos);
				imgBytes = bos.toByteArray();
			}

			return new BASE64Encoder().encode(imgBytes);
		} catch (final IOException e) {
			LOGGER.error("reading system icon for {}", file.getName(), e);
			return null;
		}
	}

	public Set<String> getAttachmentNames() {
		return Collections.unmodifiableSet(attachmentNames);
	}
}
