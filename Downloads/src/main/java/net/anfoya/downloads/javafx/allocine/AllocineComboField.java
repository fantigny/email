package net.anfoya.downloads.javafx.allocine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import javafx.util.StringConverter;
import net.anfoya.java.util.concurrent.ThreadPool;
import net.anfoya.javafx.scene.control.ComboBoxField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class AllocineComboField extends ComboBoxField<QuickSearchVo> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AllocineComboField.class);
	private static final String SEARCH_PATTERN = "http://essearch.allocine.net/fr/autocomplete?geo2=83090&q=%s";

	private volatile QuickSearchVo requestQs;
	private final AtomicLong requestQsId;

	private Callback<QuickSearchVo, Void> searchCallback;

	public AllocineComboField() {
		setPromptText("Key in a text and wait for quick search or type <Enter> for full search");
		setCellFactory(new Callback<ListView<QuickSearchVo>, ListCell<QuickSearchVo>>() {
			@Override
			public ListCell<QuickSearchVo> call(final ListView<QuickSearchVo> listView) {
				return new QuickSearchListCell();
			}
		});

		requestQs = null;
		requestQsId = new AtomicLong(0);

		setOnFieldAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				submitSearch();
			}
		});

		setOnListRequest(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				showQuickSearch();
			}
		});

		setConverter(new StringConverter<QuickSearchVo>() {
			@Override
			public String toString(final QuickSearchVo resultVo) {
				LOGGER.debug("to string \"{}\"", resultVo);
				return resultVo == null? null: resultVo.toString();
			}
			@Override
			public QuickSearchVo fromString(final String string) {
				LOGGER.debug("from string \"{}\"", string);
				return string == null? null: new QuickSearchVo(string);
			}
		});
	}

	public void setOnSearch(final Callback<QuickSearchVo, Void> callback) {
		searchCallback = callback;
	}

	public void setSearchedText(final String search) {
		setFieldValue(new QuickSearchVo(search));
	}

	private synchronized void submitSearch() {
		cancelQuickSearch();
		final QuickSearchVo qs = getFieldValue();
		if (qs == null || qs.toString().isEmpty()) {
			return;
		}

		if (searchCallback != null) {
			searchCallback.call(qs);
		}
	}

	private synchronized void showQuickSearch() {
		final QuickSearchVo currentQs = getFieldValue();
		LOGGER.debug("submit search \"{}\"", currentQs);

		if (currentQs == null) {
			// nothing to display
			cancelQuickSearch();
			return;
		}

		if (currentQs.equals(requestQs) && !getItems().isEmpty()) {
			// quick search already done
			show();
			return;
		}

		cancelQuickSearch();

		requestQs = currentQs;
		final long requestId = this.requestQsId.incrementAndGet();
		ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					requestQuickSearch(requestId, requestQs.toString());
				} catch (final InterruptedException e) {
					return;
				} catch (final Exception e) {
					LOGGER.error("loading {}", requestQs.toString(), e);
					return;
				}
			}
		});
	}

	private void requestQuickSearch(final long requestId, final String title) throws InterruptedException, MalformedURLException, IOException {
		LOGGER.debug("request quick search \"{}\" ({}) ", title, requestId);

		if (title.length() < 3) {
			// need more characters
			return;
		}

		// allow user to type more characters
		Thread.sleep(500);
		if (requestId != this.requestQsId.get()) {
			LOGGER.debug("quick search cancelled ({})", requestId);
			return;
		}

		// get a connection
		final String url = String.format(SEARCH_PATTERN, URLEncoder.encode(title, "UTF8"));
		final List<QuickSearchVo> qsResults = new ArrayList<QuickSearchVo>();
		LOGGER.info("request ({}) \"{}\"", requestId, url);
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
		if (requestId != this.requestQsId.get()) {
			LOGGER.debug("quick search cancelled ({})", requestId);
			return;
		}

		// read / parse json data
		final JsonArray jsonQsResults = new JsonParser().parse(reader).getAsJsonArray();
		for (final JsonElement jsonElement : jsonQsResults) {
			qsResults.add(new QuickSearchVo(jsonElement.getAsJsonObject()));
		}

		// launch GUI update
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (requestId != AllocineComboField.this.requestQsId.get()) {
					LOGGER.debug("quick search cancelled ({})", requestId);
					return;
				}
				LOGGER.info("displayed ({}) \"{}\"", requestId, url);
				getItems().clear();
				getItems().addAll(qsResults);
				if (!getItems().isEmpty()) {
					show();
				}
			}
		});
	}

	private void cancelQuickSearch() {
		LOGGER.debug("cancel quick search");
		hide();
		requestQsId.incrementAndGet();
	}
}
