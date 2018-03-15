package net.anfoya.mail.mime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MessageHelper {

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
		//TODO create method: public static String getSender() { ... }

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

	public static String quote(Date date, String sender, final String content) {
		return new StringBuilder()
				.append("<br><br>")
				.append("<blockheader class='gmail_quote' style='font-size:13; font-weight:bold;'>")
				.append(new SimpleDateFormat("'On' d MMM yyyy 'at' hh:mm").format(date))
				.append(", ").append(sender).append(" wrote:<br><br>")
				.append("</blockheader>")
				.append("<blockquote class='gmail_quote' style='margin:0 0 0 .8ex; border-left:1px #ccc solid; padd-left:1ex'>")
				.append(content)
				.append("</blockquote>")
				.toString();
	}

	public static String addSignature(final String html, final String signature) {
		return new StringBuffer()
				.append("<p>")
				.append(signature)
				.append("</p>")
				.append(html)
				.toString();
	}

	public static String addStyle(final String html) {
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
				.append(" padd-left:1ex !important;")
				.append(" margin: 2px 0 !important; }")
				.append("</style>")
				.append(html)
				.toString();
	}

	public static void removeMyselfFromRecipient(final String myAddress, final MimeMessage reply) throws MessagingException {
		removeMyselfFromRecipient(myAddress, reply, RecipientType.TO);
		removeMyselfFromRecipient(myAddress, reply, RecipientType.CC);
		removeMyselfFromRecipient(myAddress, reply, RecipientType.BCC);
	}

	public static void removeMyselfFromRecipient(final String myAddress, final MimeMessage reply, final RecipientType type) throws MessagingException {
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
