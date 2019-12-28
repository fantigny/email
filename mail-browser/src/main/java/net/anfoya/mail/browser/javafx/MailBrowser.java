package net.anfoya.mail.browser.javafx;

import java.util.Set;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import net.anfoya.java.undo.UndoService;
import net.anfoya.javafx.notification.NotificationService;
import net.anfoya.javafx.scene.layout.FixedSplitPane;
import net.anfoya.mail.browser.controller.Controller;
import net.anfoya.mail.browser.javafx.css.CssHelper;
import net.anfoya.mail.browser.javafx.settings.Settings;
import net.anfoya.mail.browser.javafx.thread.ThreadPane;
import net.anfoya.mail.browser.javafx.threadlist.ThreadListPane;
import net.anfoya.mail.model.Contact;
import net.anfoya.mail.model.Message;
import net.anfoya.mail.model.Section;
import net.anfoya.mail.model.Tag;
import net.anfoya.mail.model.Thread;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.javafx.scene.section.SectionListPane;

public class MailBrowser<S extends Section, T extends Tag, H extends Thread, M extends Message, C extends Contact> extends Scene {
	private static final int MIN_COLUMN_WIDTH = 200;
	public enum Mode { FULL, MINI, MICRO }

	private final Settings settings;

	private final FixedSplitPane splitPane;
	private final SectionListPane<S, T> sectionListPane;
	private final ThreadListPane<T, H> threadListPane;
	private final ThreadPane<S, T, H, M, C> threadPane;

	private final ReadOnlyObjectWrapper<Mode> modeProperty;

	private Runnable signoutCallback;

	public MailBrowser(final MailService<S, T, H, M, C> mailService
			, final NotificationService notificationService
			, final Settings settings) throws MailException {
		super(new FixedSplitPane(), Color.TRANSPARENT);
		this.settings = settings;

		modeProperty = new ReadOnlyObjectWrapper<>();

		final UndoService undoService = new UndoService();

		CssHelper.addCommonCss(this);
		CssHelper.addCss(this, "/net/anfoya/javafx/scene/control/excludebox.css");

		splitPane = (FixedSplitPane) getRoot();
		splitPane.getStyleClass().add("background");

		sectionListPane = new SectionListPane<>(mailService, undoService, settings.showExcludeBox().get());
		sectionListPane.setPrefWidth(Math.max(MIN_COLUMN_WIDTH, settings.sectionListPaneWidth().get()));
		sectionListPane.setFocusTraversable(false);
		sectionListPane.setSectionDisableWhenZero(false);
		sectionListPane.setLazyCount(true);
		splitPane.getPanes().add(sectionListPane);

		threadListPane = new ThreadListPane<>(undoService);
		threadListPane.setPrefWidth(Math.max(MIN_COLUMN_WIDTH, settings.threadListPaneWidth().get()));
		threadListPane.prefHeightProperty().bind(splitPane.heightProperty());
		splitPane.getPanes().add(threadListPane);

		threadPane = new ThreadPane<>(mailService, undoService, settings);
		threadPane.setPrefWidth(Math.max(MIN_COLUMN_WIDTH, settings.threadPaneWidth().get()));
		threadPane.setFocusTraversable(false);
		splitPane.getPanes().add(threadPane);

		splitPane.setOnKeyPressed(e -> toggleViewMode(e));

		final Controller<S, T, H, M, C> controller = new Controller<>(
				mailService
				, notificationService
				, undoService
				, settings);
		controller.setMailBrowser(this);
		controller.setSectionListPane(sectionListPane);
		controller.setThreadListPane(threadListPane);
		controller.addThreadPane(threadPane);
		controller.init();

		windowProperty().addListener((ov, o, n) -> {
			if (n != null) {
				setMode(Mode.valueOf(settings.browserMode().get()));
			}
		});
	}

	public void addOnModeChange(Runnable callback) {
		modeProperty.addListener((ov, o, n) -> callback.run());
	}

	public ReadOnlyObjectProperty<Mode> modeProperty() {
		return modeProperty.getReadOnlyProperty();
	}

	public void setMode(Mode mode) {
		modeProperty.set(mode);
		threadListPane.setMode(mode);

		Pane resizable;
		Pane[] visiblePanes;
		switch(mode) {
		case MICRO:
			visiblePanes = new Pane[] { threadListPane };
			resizable = threadListPane;
			break;
		case MINI:
			visiblePanes = new Pane[] { sectionListPane, threadListPane };
			resizable = threadListPane;
			break;
		case FULL: default:
			visiblePanes = new Pane[] { sectionListPane, threadListPane, threadPane };
			resizable = threadPane;
			break;
		}

		splitPane.setVisiblePanes(visiblePanes);
		splitPane.setResizableWithParent(resizable);

		if (getWindow() != null) {
			final Stage stage = (Stage) getWindow();
			stage.setWidth(splitPane.computePrefWidth());
			stage.setMinWidth(splitPane.computeMinWidth());
		}
	}

	public void setOnSignout(final Runnable callback) {
		signoutCallback = callback;
	}

	public void signout( ) {
		signoutCallback.run();
	}

	private void toggleViewMode(final KeyEvent e) {
		final boolean left = e.getCode() == KeyCode.LEFT;
		final boolean right = e.getCode() == KeyCode.RIGHT;
		if (left || right) {
			e.consume();
		} else {
			return;
		}

		final int nbVisible = splitPane.getVisiblePanes().size();
		final Mode mode;
		if (right && nbVisible == 2) {
			mode = Mode.FULL;
		} else if (right && nbVisible == 1
				|| left && nbVisible == 3) {
			mode = Mode.MINI;
		} else if (left && nbVisible == 2) {
			mode = Mode.MICRO;
		} else {
			return;
		}
		setMode(mode);
		settings.browserMode().set(mode.toString());
	}

	public void saveSettings() {
		settings.sectionListPaneWidth().set(sectionListPane.getWidth());
		settings.threadListPaneWidth().set(threadListPane.getWidth());
		settings.threadPaneWidth().set(threadPane.getWidth());

		final Set<T> tags = sectionListPane.getIncludedOrSelectedTags();
		if (tags.size() == 1) {
			settings.sectionName().set(sectionListPane.getSelectedSection().getName());
			settings.tagName().set(tags.iterator().next().getName());
		} else {
			settings.sectionName().set("");
			settings.tagName().set("");
		}
	}
}
