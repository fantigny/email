package net.anfoya.mail.browser.javafx.threadlist;

import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import net.anfoya.java.undo.UndoService;
import net.anfoya.java.util.VoidCallback;
import net.anfoya.javafx.scene.animation.DelayTimeline;
import net.anfoya.javafx.scene.control.ResetTextField;
import net.anfoya.javafx.scene.dnd.DndHelper;
import net.anfoya.mail.browser.controller.vo.TagForThreadsVo;
import net.anfoya.mail.browser.javafx.BrowserToolBar;
import net.anfoya.mail.browser.javafx.MailBrowser.Mode;
import net.anfoya.mail.browser.javafx.thread.ThreadToolBar;
import net.anfoya.mail.model.SimpleTag;
import net.anfoya.mail.model.SimpleThread.SortField;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.Tag;
import net.anfoya.mail.service.Thread;
import net.anfoya.tag.javafx.scene.dnd.ExtItemDropPane;

public class ThreadListPane<T extends Tag, H extends Thread> extends BorderPane {
	public static final DataFormat DND_THREADS_DATA_FORMAT = new DataFormat("DND_THREADS_DATA_FORMAT");

	private final ThreadList<T, H> threadList;
	private final ResetTextField patternField;

	private final BrowserToolBar toolBar;
	private final BooleanProperty showToolBar;

	private final ThreadListDropPane threadListDropPane;

	private DelayTimeline patternDelay;

	private VoidCallback<Set<H>> archiveCallback;
	private VoidCallback<Set<H>> replyCallback;
	private VoidCallback<Set<H>> replyAllCallback;
	private VoidCallback<Set<H>> forwardCallback;
	private VoidCallback<Set<H>> toggleFlagCallback;
	private VoidCallback<Set<H>> trashCallback;
	private VoidCallback<Set<H>> toggleSpamCallback;

	private VoidCallback<Set<H>> openCallback;

	private VoidCallback<TagForThreadsVo<T, H>> addTagForThreadsCallBack;
	private VoidCallback<TagForThreadsVo<T, H>> createTagForThreadsCallBack;

	private final DisconnectedPane disconnectedPane;


	public ThreadListPane(final UndoService undoService) throws MailException {
		getStyleClass().add("thread-list-pane");

		patternField = new ResetTextField();
		patternField.setPromptText("thread search");

		threadList = new ThreadList<>();
		threadList.setOnMouseClicked(e -> {
			if (e.getClickCount() > 1) {
				openCallback.call(getSelectedThreads());
			}
		});
		threadList.setOnDragDetected(e -> {
			final ClipboardContent content = new ClipboardContent();
			content.put(ExtItemDropPane.ADD_TAG_DATA_FORMAT, "");
			content.put(DND_THREADS_DATA_FORMAT, "");

			int count = 0;
			final StackPane stackPane = new StackPane();
			final ThreadListCell<H> cell = new ThreadListCell<>();
			for (final H t : getSelectedThreads()) {
				final Pane grid = cell.buildGridPane(t);
				grid.getStyleClass().add("thread-list-cell-dnd");
				grid.setTranslateX(4 * count);
				grid.setTranslateY(8 * count);
				stackPane.getChildren().add(grid);
				count++;
			}
			final Image image = new DndHelper(getScene().getStylesheets()).toImage(stackPane);

			final Dragboard db = threadList.startDragAndDrop(TransferMode.ANY);
			db.setContent(content);
			db.setDragView(image, image.getWidth() / 2, image.getHeight() / 2);
		});
		threadList.setOnDragDone(e -> {
			final Dragboard db = e.getDragboard();
			if (db.hasContent(Tag.TAG_DATA_FORMAT)) {
				@SuppressWarnings("unchecked") final T tag = (T) db.getContent(Tag.TAG_DATA_FORMAT);
				addTagForThreadsCallBack.call(new TagForThreadsVo<>(tag, getSelectedThreads()));
			} else if (db.hasContent(ExtItemDropPane.TAG_NAME_DATA_FORMAT)) {
				final String name = (String) db.getContent(ExtItemDropPane.TAG_NAME_DATA_FORMAT);
				@SuppressWarnings("unchecked") final T tag = (T) new SimpleTag(null, name, null, false);
				createTagForThreadsCallBack.call(new TagForThreadsVo<>(tag, getSelectedThreads()));
			}
			e.consume();
		});

		final StackPane centerPane = new StackPane(threadList);
		centerPane.setAlignment(Pos.BOTTOM_CENTER);
		setCenter(centerPane);

		centerPane.getChildren().add(new UndoPane(undoService));

		disconnectedPane = new DisconnectedPane();
		centerPane.getChildren().add(disconnectedPane);

		final VBox topBox = new VBox();
		setTop(topBox);

		toolBar = new BrowserToolBar();
		toolBar.setVisibles(true, false, false);

		final HBox firstLineBox = new HBox(patternField, toolBar);
		firstLineBox.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(patternField, Priority.ALWAYS);
		topBox.getChildren().add(firstLineBox);

		final ThreadToolBar toolbar = new ThreadToolBar();
		toolbar.setFocusTraversable(false);
		toolbar.setPadding(new Insets(0));
		toolbar.setOnReply(() -> replyCallback.call(getSelectedThreads()));
		toolbar.setOnReplyAll(() -> replyAllCallback.call(getSelectedThreads()));
		toolbar.setOnForward(() -> forwardCallback.call(getSelectedThreads()));
		toolbar.setOnToggleFlag(() -> toggleFlagCallback.call(getSelectedThreads()));
		toolbar.setOnArchive(() -> archiveCallback.call(getSelectedThreads()));
		toolbar.setOnTrash(() -> trashCallback.call(getSelectedThreads()));
		toolbar.setOnSpam(() -> toggleSpamCallback.call(getSelectedThreads()));

		showToolBar = new SimpleBooleanProperty();
		showToolBar.addListener((ov, o, n) -> {
			if (n) {
				topBox.getChildren().add(toolbar);
			} else {
				topBox.getChildren().remove(toolbar);
			}
		});

		threadListDropPane = new ThreadListDropPane();
		threadListDropPane.prefWidthProperty().bind(centerPane.widthProperty());
		threadListDropPane.setOnArchive(e -> archiveCallback.call(getSelectedThreads()));
		threadListDropPane.setOnForward(e -> forwardCallback.call(getSelectedThreads()));
		threadListDropPane.setOnReply(all -> {
			if (all) {
				replyCallback.call(getSelectedThreads());
			} else {
				replyAllCallback.call(getSelectedThreads());
			}
		});

		centerPane.setOnDragEntered(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)
					&& !centerPane.getChildren().contains(threadListDropPane)) {
				centerPane.getChildren().add(threadListDropPane);
			}

		});
		centerPane.setOnDragExited(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)
					&& centerPane.getChildren().contains(threadListDropPane)) {
				centerPane.getChildren().remove(threadListDropPane);
			}
		});

		final Label sortLabel = new Label(SortField.DATE.toString().toLowerCase());
		sortLabel.setOnMouseClicked(e -> {
			final SortField field;
			if (sortLabel.getText().equalsIgnoreCase(SortField.DATE.toString())) {
				field = SortField.SENDER;
			} else {
				field = SortField.DATE;
			}
			sortLabel.setText(field.toString().toLowerCase());
			threadList.sortBy(field);
		});

		final FlowPane sortPane = new FlowPane(new Label("sorted by "), sortLabel);
		sortPane.setAlignment(Pos.CENTER);
		setBottom(sortPane);
		setMargin(sortPane, new Insets(5));
	}

	public void setOnArchive(VoidCallback<Set<H>> callback) {
		archiveCallback = callback;
		threadList.setOnArchive(callback);
	}

	public void setOnReply(VoidCallback<Set<H>> callback) {
		replyCallback = callback;
	}

	public void setOnReplyAll(VoidCallback<Set<H>> callback) {
		replyAllCallback = callback;
	}

	public void setOnForward(VoidCallback<Set<H>> callback) {
		forwardCallback = callback;
	}

	public void setOnToggleFlag(VoidCallback<Set<H>> callback) {
		toggleFlagCallback = callback;
	}

	public void setOnTrash(VoidCallback<Set<H>> callback) {
		trashCallback = callback;
	}

	public void setOnToggleSpam(VoidCallback<Set<H>> callback) {
		toggleSpamCallback = callback;
	}

	public void setOnAddTagForThreads(VoidCallback<TagForThreadsVo<T, H>> callback) {
		addTagForThreadsCallBack = callback;
	}

	public String getSearchPattern() {
		return patternField.getText();
	}

	public int getThreadCount() {
		return threadList.getItems().size();
	}

	public Set<H> getSelectedThreads() {
		return threadList.getSelectedThreads();
	}

	public ObservableList<H> getItems() {
		return threadList.getItems();
	}

	public void setOnView(final VoidCallback<Set<H>> callback) {
		threadList.setOnSelect(callback);
	}

	public void setOnOpen(final VoidCallback<Set<H>> callback) {
		this.openCallback = callback;
	}

	public void setOnLoad(final Runnable callback) {
		threadList.setOnLoad(callback);
	}

	public Set<String> getThreadsTagIds() {
		return getItems()
				.stream()
				.flatMap(t -> t.getTagIds().stream())
				.collect(Collectors.toSet());
	}

	public void setOnUpdatePattern(final Runnable callback) {
		patternField.textProperty().addListener((ov, o, n) -> {
			if (patternDelay == null) {
				patternDelay = new DelayTimeline(Duration.millis(500), e -> callback.run());
			}
			patternDelay.playFromStart();
		});
	}

	public void setMode(Mode mode) {
		if (mode == Mode.FULL) {
			toolBar.setVisibles(true, false, false);
		} else {
			toolBar.setVisibles(true, true, true);
		}
	}

	public void setOnCreateTagForThreads(VoidCallback<TagForThreadsVo<T, H>> callBack) {
		createTagForThreadsCallBack = callBack;
	}

	public void setOnCompose(Runnable callback) {
		toolBar.setOnCompose(callback);
	}

	public void setAll(Set<H> threads, boolean unread) {
		threadList.setAll(threads, unread);
	}

	public BooleanProperty disconnected() {
		return disconnectedPane.disconnected();
	}

	public void setOnReconnect(Runnable callback) {
		disconnectedPane.setOnReconnect(callback);
	}

	public void setOnShowSettings(Runnable callback) {
		toolBar.setOnShowSettings(callback);
	}

	public void setOnSignout(Runnable callback) {
		toolBar.setOnSignout(callback);
	}

	public BooleanProperty showToolBarProperty() {
		return showToolBar;
	}
}
