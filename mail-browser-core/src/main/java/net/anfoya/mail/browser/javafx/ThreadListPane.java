package net.anfoya.mail.browser.javafx;

import java.util.LinkedHashSet;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import net.anfoya.javafx.scene.control.Title;
import net.anfoya.mail.browser.javafx.dnd.ThreadListDropPane;
import net.anfoya.mail.model.SimpleMessage;
import net.anfoya.mail.model.SimpleThread;
import net.anfoya.mail.model.SimpleThread.SortOrder;
import net.anfoya.mail.service.MailException;
import net.anfoya.mail.service.MailService;
import net.anfoya.tag.model.SimpleSection;
import net.anfoya.tag.model.SimpleTag;

public class ThreadListPane<S extends SimpleSection, T extends SimpleTag, H extends SimpleThread> extends BorderPane {
	public static final DataFormat DND_THREADS_DATA_FORMAT = new DataFormat("Set<" + SimpleThread.class.getName() + ">");

	private final MailService<S, T, H, ? extends SimpleMessage> mailService;
	private final ThreadList<S, T, H> threadList;
	private final TextField namePatternField;

	public ThreadListPane(final MailService<S, T, H, ? extends SimpleMessage> mailService) {
		this.mailService = mailService;
		
		final BorderPane patternPane = new BorderPane();
		setTop(patternPane);

		final Title title = new Title("Threads");
		title.setPadding(new Insets(0, 10, 0, 5));
		patternPane.setLeft(title);

		namePatternField = new TextField();
		namePatternField.setPromptText("search");
		namePatternField.textProperty().addListener((ChangeListener<String>) (ov, oldPattern, newPattern) -> threadList.refreshWithPattern(newPattern));
		patternPane.setCenter(namePatternField);
		BorderPane.setMargin(namePatternField, new Insets(0, 5, 0, 5));

		final Button delPatternButton = new Button("X");
		delPatternButton.setOnAction(event -> namePatternField.textProperty().set(""));
		patternPane.setRight(delPatternButton);

		final StackPane stackPane = new StackPane();
		stackPane.setAlignment(Pos.BOTTOM_CENTER);

		final ThreadListDropPane<H> threadListDropPane = new ThreadListDropPane<H>(mailService);
		threadListDropPane.prefWidthProperty().bind(stackPane.widthProperty());

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
		setCenter(stackPane);

		threadList = new ThreadList<S, T, H>(mailService);
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
			threadList.refresh();
			event.consume();
		});
		stackPane.getChildren().add(threadList);

		final ToggleGroup toggleGroup = new ToggleGroup();

		final RadioButton nameSortButton = new RadioButton("name ");
		nameSortButton.setToggleGroup(toggleGroup);

		final RadioButton dateSortButton = new RadioButton("date");
		dateSortButton.setToggleGroup(toggleGroup);

		toggleGroup.selectedToggleProperty().addListener((ChangeListener<Toggle>) (ov, oldVal, newVal) -> threadList.refreshWithOrder(nameSortButton.isSelected()
				? SortOrder.SUBJECT
				: SortOrder.DATE));
		dateSortButton.setSelected(true);

		final HBox box = new HBox(new Label("Sort by: "), nameSortButton, dateSortButton);
		box.setAlignment(Pos.BASELINE_CENTER);
		box.setSpacing(5);
		setBottom(box);

		setMargin(patternPane, new Insets(5));
		setMargin(threadList, new Insets(0, 5, 0, 5));
		setMargin(box, new Insets(5));
	}

	public String getNamePattern() {
		return namePatternField.getText();
	}

	public void refreshWithTags(final Set<T> tags, final Set<T> includes, final Set<T> excludes) {
		threadList.refresh(tags, includes, excludes);
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

	public boolean isRefreshing() {
		return threadList.isRefreshing();
	}

	public ObservableList<H> getItems() {
		return threadList.getItems();
	}

	public void addSelectionListener(final ChangeListener<H> listener) {
		threadList.getSelectionModel().selectedItemProperty().addListener(listener);
	}

	public void addChangeListener(final ListChangeListener<H> listener) {
		threadList.getItems().addListener(listener);
	}

	public Set<T> getThreadTags() {
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
}
