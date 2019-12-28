package net.anfoya.mail.model;

import javafx.scene.image.Image;
import net.anfoya.mail.service.MailServiceInfo;

public abstract class SimpleInfo implements MailServiceInfo {
	private final String name;
	private final String iconPath;


	public SimpleInfo(String name, String iconPath) {
		this.name = name;
		this.iconPath = iconPath;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Image getIcon() {
		return new Image(SimpleInfo.class.getResourceAsStream(iconPath));
	}
}