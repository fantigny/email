package net.anfoya.mail.browser.javafx.threadlist;

import java.util.LinkedHashSet;
import java.util.Set;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.ResetTextField;
import net.anfoya.javafx.scene.control.Title;
import net.anfoya.mail.browser.javafx.message.MessageComposer;
import net.anfoya.mail.model.SimpleContact;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.model.SimpleThread.SortOrder;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.javafx.scene.dnd.DndFormat;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.mail.model.SimpleTag;

public class ThreadListPane<S extends SimpleSection, T extends SimpleTag, H extends SimpleThread, M extends SimpleMessage, C extends SimpleContact> extends BorderPane {
	public static final DataFormat DND_THREADS_DATA_FORMAT = new DataFormat("Set<" + SimpleThread.class.getName() + ">");

	private final MailService<S, T, H, M, C> mailService;
	private final ThreadList<S, T, H, M, C> threadList;
	private final ResetTextField namePatternField;

	private EventHandler<ActionEvent> updateHandler;

	private ThreadListDropPane<T, H, M, C> threadListDropPane;

	protected Timeline expandDelay;

	public ThreadListPane(final MailService<S, T, H, M, C> mailService) {
		this.mailService = mailService;

		final StackPane stackPane = new StackPane();
		stackPane.setAlignment(Pos.BOTTOM_CENTER);
		setCenter(stackPane);

		threadListDropPane = new ThreadListDropPane<T, H, M, C>(mailService);
		threadListDropPane.prefWidthProperty().bind(stackPane.widthProperty());

		threadList = new ThreadList<S, T, H, M, C>(mailService);
		threadList.setOnDragDetected(event -> {
			final Set<H> threads = getSelectedThreads();
			if (threads.size() == 0) {
				return;
			}
	        final ClipboardContent content = new ClipboardContent();
	        content.put(DND_THREADS_DATA_FORMAT, threads);
	        final Dragboard db = threadList.startDragAndDrop(TransferMode.ANY);
	        db.setContent(content);
		});
		threadList.setOnDragDone(event -> {
			final Dragboard db = event.getDragboard();
			if (db.hasContent(ThreadListPane.DND_THREADS_DATA_FORMAT)
					&& db.hasContent(DndFormat.TAG_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) db.getContent(DND_THREADS_DATA_FORMAT);
				@SuppressWarnings("unchecked")
				final T tag = (T) db.getContent(DndFormat.TAG_DATA_FORMAT);
				addTagForThreads(tag, threads);
				event.consume();
			} else if (db.hasContent(ThreadListPane.DND_THREADS_DATA_FORMAT)
					&& db.hasContent(DndFormat.TAG_NAME_DATA_FORMAT)) {
				@SuppressWarnings("unchecked")
				final Set<H> threads = (Set<H>) db.getContent(DND_THREADS_DATA_FORMAT);
				final String name = (String) db.getContent(DndFormat.TAG_NAME_DATA_FORMAT);
				createTagForThreads(name, threads);
				event.consume();
			}
		});
		stackPane.getChildren().add(threadList);

		stackPane.setOnDragEntered(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)
					&& !stackPane.getChildren().contains(threadListDropPane)) {
				stackPane.getChildren().add(threadListDropPane);
			}

		});
		stackPane.setOnDragExited(event -> {
			if (event.getDragboard().hasContent(DND_THREADS_DATA_FORMAT)
					&& stackPane.getChildren().contains(threadListDropPane)) {
				stackPane.getChildren().remove(threadListDropPane);
			}
		});

		final ToggleGroup toggleGroup = new ToggleGroup();

		final RadioButton nameSortButton = new RadioButton("sender");
		nameSortButton.setToggleGroup(toggleGroup);

		final RadioButton dateSortButton = new RadioButton("date");
		dateSortButton.setToggleGroup(toggleGroup);

		toggleGroup.selectedToggleProperty().addListener((ChangeListener<Toggle>) (ov, oldVal, newVal) -> threadList.refreshWithOrder(nameSortButton.isSelected()
				? SortOrder.SENDER
				: SortOrder.DATE));
		dateSortButton.setSelected(true);

		final HBox box = new HBox(new Label("Sort by: "), nameSortButton, dateSortButton);
		box.setMinHeight(26);
		box.setAlignment(Pos.CENTER);
		box.setSpacing(5);
		setBottom(box);
		final BorderPane patternPane = new BorderPane();
		setTop(patternPane);

		final Title title = new Title("Mail");
		title.setPadding(new Insets(0, 5, 0, 0));
		patternPane.setLeft(title);

		namePatternField = new ResetTextField();
		namePatternField.setPromptText("search");
		patternPane.setCenter(namePatternField);
		BorderPane.setMargin(namePatternField, new Insets(0, 5, 0, 0));

		final Button addButton = new Button("+");
		addButton.setOnAction(event -> {
			try {
				new MessageComposer<M, C>(mailService, updateHandler).newMessage();
			} catch (final Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		patternPane.setRight(addButton);

		setMargin(patternPane, new Insets(5, 0, 5, 0));
		setMargin(threadList, new Insets(0, 5, 0, 5));
		setMargin(box, new Insets(5));
	}

	private void addTagForThreads(final T tag, final Set<H> threads) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				mailService.addTagForThreads(tag, threads);
				return null;
			}
		};
		task.setOnFailed(event -> // TODO Auto-generated catch block
			event.getSource().getException().printStackTrace(System.out));
		task.setOnSucceeded(event -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);
	}

	private void createTagForThreads(final String name, final Set<H> threads) {
		final Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				final T tag = mailService.addTag(name);
				mailService.addTagForThreads(tag, threads);
				return null;
			}
		};
		task.setOnFailed(event -> // TODO Auto-generated catch block
			event.getSource().getException().printStackTrace(System.out));
		task.setOnSucceeded(event -> updateHandler.handle(null));
		ThreadPool.getInstance().submitHigh(task);
	}

	public String getNamePattern() {
		return namePatternField.getText();
	}

	public void refreshWithTags(final Set<T> includes, final Set<T> excludes) {
		threadList.refresh(includes, excludes);
	}

	public void refreshWithPage(final int page) {
		threadList.refreshWithPage(page);
	}

	public int getThreadCount() {
		return threadList.getItems().size();
	}

	public Set<T> getMoviesTags() {
		return threadList.getThreadsTags();
	}

	public Set<H> getSelectedThreads() {
		return threadList.getSelectedThreads();
	}

	public ObservableList<H> getItems() {
		return threadList.getItems();
	}

	public void setOnSelectThread(final EventHandler<ActionEvent> handler) {
		threadList.setOnSelectThreads(handler);
	}

	public void setOnLoadThreadList(final EventHandler<ActionEvent> handler) {
		threadList.setOnLoad(handler);
	}

	public Set<T> getThreadsTags() {
		try {
			final Set<T> tags = new LinkedHashSet<T>();
			for(final H t: getItems()) {
				for(final String id: t.getTagIds()) {
					tags.add(mailService.getTag(id));
				}
			}
			return tags;
		} catch (final MailException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public void setOnUpdatePattern(final EventHandler<ActionEvent> handler) {
		namePatternField.textProperty().addListener((ov, oldVal, newVal) -> {
			if (expandDelay != null) {
				expandDelay.stop();
			}
			expandDelay = new Timeline(new KeyFrame(Duration.millis(500), new EventHandler<ActionEvent>() {
				@Override
			    public void handle(final ActionEvent event) {
					threadList.refreshWithPattern(newVal);
					handler.handle(null);
		    		return;
			    }
			}));
			expandDelay.setCycleCount(1);
			expandDelay.play();
		});
	}

	public void setOnUpdateThread(final EventHandler<ActionEvent> handler) {
		updateHandler = handler;
		threadListDropPane.setOnUpdate(handler);
		threadList.setOnUpdate(handler);
	}
}
