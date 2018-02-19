package net.anfoya.mail.browser.controller;

import java.util.LinkedList;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.service.Contact;
import net.anfoya.mail.service.Message;
import net.anfoya.mail.service.Section;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;

/*
 * Supervisor is here to keep the controller in line with user expectations...
 * it looks at the user's intention and change the controller's behaviour accordingly
 * 
 * */
public class Supervisor<S extends Section, T extends Tag, H extends Thread, M extends Message, C extends Contact> {
	private static final int MAX_HISTORY = 5;
	
	private final Controller<S, T, H, M, C> controller;
	
	private final ObjectProperty<Intention> intention;
	private final ObjectProperty<Target> target;
	private final LinkedList<Intention> story;
	
	public Supervisor(final Controller<S, T, H, M, C> controller, Settings settings) {
		this.controller = controller;
		
		intention = new SimpleObjectProperty<>();
		target = new SimpleObjectProperty<>();		
		story = new LinkedList<>();
		
		settings.intention().bind(intention);
		settings.target().bind(target);
		
		add(settings.intention().get());
	}
	
	public void add(Intention intention) {
		this.intention.set(intention);

		synchronized (story) {
			story.addLast(intention);
			if (story.size() > MAX_HISTORY) {
				story.removeFirst();
			}
		}
		
		computeTarget();
	}
	
	private void computeTarget() {
		final Target target;
		switch(story.getLast()) {
		default: target = Target.thread;  
		}
		
		this.target.set(target);
	}

	private void guessIntention() {
		intention.set(story.getFirst());
	}
}
