package net.anfoya.mail.mime;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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
import javax.mail.Message.RecipientType;
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

import net.anfoya.mail.browser.javafx.settings.Settings;
import sun.misc.BASE64Encoder;
import sun.misc.IOUtils;

public class MessageHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageHelper.class);
	private static final String TEMP = System.getProperty("java.io.tmpdir") + File.separatorChar;
	private static final String SYS_ICON = "/net/anfoya/mail/sys/%s.png";

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

	public static String getName(final InternetAddress address) {
		final String name;
		if (address.getPersonal() != null) {
			name = address.getPersonal();
		} else {
			name = address.getAddress();
		}

		return name
				.split("\\(")[0]
				.replaceAll("[\\r,\\n,']", "")
				.replaceAll("\\t", " ")
				.trim();
	}

	public static List<String> getNames(final Address[] addresses) {
		final List<String> names = new ArrayList<String>();
		for(final Address a: addresses) {
			if (a.getType().equalsIgnoreCase("rfc822")) {
				names.add(getName((InternetAddress) a));
			}
		}

		return names;
	}

	public static String quote(String html) {
		return new StringBuffer()
				.append("<br><br>")
				.append("<blockquote class='gmail_quote' style='margin:0 0 0 .8ex;border-left:1px #ccc solid;padding-left:1ex'>")
				.append(html)
				.append("</blockquote>")
				.toString();
	}

	public static String addSignature(String html) {
		return new StringBuffer()
				.append("<p>")
				.append(Settings.getSettings().htmlSignature().get())
				.append("</p>")
				.append(html)
				.toString();
	}

	public static String addStyle(String html) {
		return new StringBuffer()
				.append("<style>")
				.append("html,body {")
				.append(" line-height: 1em !important;")
				.append(" font-size: 14px !important;")
				.append(" font-size: 14px !important;")
				.append(" font-family: Lucida Grande !important;")
				.append(" color: #222222 !important;")
				.append(" background-color: #FDFDFD !important; }")
				.append("p {")
				.append(" padding-left:1ex !important;")
				.append(" margin: 2px 0 !important; }")
				.append("</style>")
				.append(html)
				.toString();
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
			final boolean isPlainContent = type.contains("text/plain");
			if (isHtml && isPlainContent) {
				LOGGER.info("discarding {}", type);
				return new StringBuilder();
			}

			final StringBuilder html = new StringBuilder();
			if (isPlainContent) {
				html.append("<pre>").append(content).append("</pre>");
			} else {
				String encoding;
				try { encoding = part.getHeader("Content-Transfer-Encoding")[0]; }
				catch(final Exception e) { encoding = MimeUtility.getEncoding(part.getDataHandler()); }

				LOGGER.info("decoding {}, {}", type, encoding);
				final byte[] bytes = ((String) content).getBytes();
				if (bytes.length > 0) {
					try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
							InputStream decodedStream = MimeUtility.decode(byteStream, encoding);
							BufferedReader reader = new BufferedReader(new InputStreamReader(decodedStream))) {
						reader.lines().forEach(s -> html.append(s));
					} catch (final Exception e) {
						html.append(content);
					}
				}
			}
			return html;
		}
		if (part instanceof MimeBodyPart && content instanceof BASE64DecoderStream) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			if (type.contains("text/calendar")) {
				final File file = File.createTempFile("event-", ".ics", new File(TEMP));
				LOGGER.info("saving inline calendar {}", file);
				bodyPart.saveFile(file);
				return buildImgAnchor(file);
			}
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
						return buildImgAnchor(file);
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

	private StringBuilder buildImgAnchor(File file) throws UnsupportedEncodingException {
		return new StringBuilder()
				.append("<a href='").append(file.toURI()).append("'>")
				.append("<table><tr><td align='center'>")
					.append("<img width='32' height='32' title='").append(file.getName()).append("' ")
					.append("src='data:image/png;base64,").append(getBase64SystemIcon(file)).append("'>")
					.append("<table><tr><td align='center'>")
				.append("</td></tr><tr><td>")
					.append(file.getName())
				.append("</td></tr></table>")
			.append("</a>");
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
			return "";
		}
	}

	public Set<String> getAttachmentNames() {
		return Collections.unmodifiableSet(attachmentNames);
	}

	public void removeMyselfFromRecipient(String myAddress, MimeMessage reply) throws MessagingException {
		removeMyselfFromRecipient(myAddress, reply, RecipientType.TO);
		removeMyselfFromRecipient(myAddress, reply, RecipientType.CC);
		removeMyselfFromRecipient(myAddress, reply, RecipientType.BCC);
	}

	public void removeMyselfFromRecipient(String myAddress, MimeMessage reply, RecipientType type) throws MessagingException {
		final Address[] addresses = reply.getRecipients(type);
		if (addresses == null || addresses.length == 0) {
			return;
		}

		final List<Address> to = new ArrayList<Address>();
		for(final Address a: addresses) {
			if (!myAddress.equals(((InternetAddress) a).getAddress())) {
				to.add(a);
			}
		}
		reply.setRecipients(type, to.toArray(new Address[0]));
		reply.saveChanges();
	}
}
