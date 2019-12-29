package net.anfoya.mail.mime;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.swing.Icon;
import javax.swing.JFileChooser;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.anfoya.java.io.TmpFileHandler;

public class MessageReader {
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageReader.class);
	private static final String SYS_ICON = "/net/anfoya/mail/sys/%s.png";

	private final Map<String, String> cidUris;
	private final Set<String> attachmentNames;

	private final TmpFileHandler tmp;

	public MessageReader() {
		cidUris = new HashMap<>();
		attachmentNames = new LinkedHashSet<>();
		tmp = TmpFileHandler.getDefault();
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
		isHtml = isHtml || part.isMimeType("multipart/alternative");
		LOGGER.info("isHtml({}) type({}) part/content({}/{})", isHtml, part.getContentType(), part.getClass(), content.getClass());
		if (part instanceof Multipart || part.isMimeType("multipart/*")) {
			final Multipart parts = (Multipart) content;
			final StringBuilder html = new StringBuilder();
			for(int i=0, n=parts.getCount(); i<n; i++) {
				html.append(toHtml(parts.getBodyPart(i), isHtml));
			}
			return html;
		}
		if (content instanceof String) {
			final boolean isPlainContent = part.isMimeType("text/plain");
			if (isHtml && isPlainContent) {
				LOGGER.info("discard {}", part.getContentType());
				return new StringBuilder();
			}

			if (isPlainContent) {
				return new StringBuilder()
						.append("<pre>")
						.append(content)
						.append("</pre>");
			} else {
				// TODO: printable text encoding
				//				String encoding;
				//				try { encoding = part.getHeader("Content-Transfer-Encoding")[0]; }
				//				catch(final Exception e) { encoding = MimeUtility.getEncoding(part.getDataHandler()); }
				//
				//				LOGGER.info("decode {}, {}", type, encoding);
				//				final byte[] bytes = ((String) content).getBytes();
				//				if (bytes.length > 0) {
				//					try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
				//							InputStream decodedStream = MimeUtility.decode(byteStream, encoding);
				//							BufferedReader reader = new BufferedReader(new InputStreamReader(decodedStream))) {
				//						reader.lines().forEach(s -> html.append(s));
				//					} catch (final Exception e) {
				//						html.append(content);
				//					}
				//				}
				//				System.out.println(content);
				return new StringBuilder().append(content);
			}
		}
		if (part instanceof MimeBodyPart
				/*&& content instanceof BASE64DecoderStream*/) {
			final MimeBodyPart bodyPart = (MimeBodyPart) part;
			if (part.isMimeType("text/calendar")) {
				final File file = tmp.createTempFile("event-", ".ics");
				LOGGER.info("save inline calendar {}", file);
				bodyPart.saveFile(file);
				return buildImgAnchor(file);
			}
			if (bodyPart.getContentID() != null
					&& !MimeBodyPart.ATTACHMENT.equals(bodyPart.getDisposition())) {
				final String cid = bodyPart.getContentID().replaceAll("[<,>]", "");
				final File file = tmp.createTempFile(cid);
				LOGGER.info("save cid {}", file);
				bodyPart.saveFile(file);
				cidUris.put(cid, file.toURI().toString());
				return new StringBuilder();
			}
			if (bodyPart.getFileName() != null) {
				final String filename = MimeUtility.decodeText(bodyPart.getFileName());
				if (MimeBodyPart.INLINE.equals(bodyPart.getDisposition())) {
					final File file = tmp.createTempFile(filename);
					LOGGER.info("save inline file {}", file);
					bodyPart.saveFile(file);
					if (part.isMimeType("image/*")) {
						return new StringBuilder("<img src='").append(file.toURI()).append("'>");
					} else {
						return buildImgAnchor(file);
					}
				}
				if (MimeBodyPart.ATTACHMENT.equals(bodyPart.getDisposition())) {
					LOGGER.info("save reference to attachment {}", filename);
					attachmentNames.add(filename);
					return new StringBuilder();
				}
			}
		}

		LOGGER.warn("not handled {}/{}", part.getContentType(), content.getClass());
		return new StringBuilder();
	}

	private StringBuilder buildImgAnchor(final File file) throws UnsupportedEncodingException {
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

	private String getBase64SystemIcon(final File file) {
		try {
			final byte[] imgBytes;
			final String iconFileName = String.format(SYS_ICON, file.getName().substring(file.getName().lastIndexOf(".") + 1));
			if (getClass().getResource(iconFileName) != null) {
				final InputStream in = getClass().getResourceAsStream(iconFileName);
				imgBytes = IOUtils.toByteArray(in);
			} else {
				final Icon icon = new JFileChooser().getIcon(file);
				final BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TRANSLUCENT);
				icon.paintIcon(null, bi.createGraphics(), 0, 0);
				final ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ImageIO.write(bi, "png", bos);
				imgBytes = bos.toByteArray();
			}

			return new Base64().encodeAsString(imgBytes);
		} catch (final IOException e) {
			LOGGER.error("read system icon for {}", file.getName(), e);
			return "";
		}
	}

	public Set<String> getAttachmentNames() {
		return Collections.unmodifiableSet(attachmentNames);
	}
}
