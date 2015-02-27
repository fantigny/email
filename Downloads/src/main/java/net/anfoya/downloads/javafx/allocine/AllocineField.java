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

public class AllocineField extends ComboBoxField<QuickSearchVo> {
	private static final Logger LOGGER = LoggerFactory.getLogger(AllocineField.class);
	private static final String SEARCH_PATTERN = "http://essearch.allocine.net/fr/autocomplete?geo2=83090&q=%s";
	private volatile QuickSearchVo requestedVo;
	private final AtomicLong requestTime;

	private Callback<QuickSearchVo, Void> searchCallback;

	public AllocineField() {
		setPromptText("Key in a text and wait for quick search or type <Enter> for full search");
		setCellFactory(new Callback<ListView<QuickSearchVo>, ListCell<QuickSearchVo>>() {
			@Override
			public ListCell<QuickSearchVo> call(final ListView<QuickSearchVo> listView) {
				return new QuickSearchListCell();
			}
		});

		requestedVo = null;
		requestTime = new AtomicLong(0);

		setOnFieldAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				submitSearch();
			}
		});

		setOnListRequest(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent event) {
				requestList();
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
		cancelListRequest();
		final QuickSearchVo qs = getFieldValue();
		if (qs == null || qs.toString().isEmpty()) {
			return;
		}

		if (searchCallback != null) {
			searchCallback.call(qs);
		}
	}

	private synchronized void requestList() {
		final QuickSearchVo vo = getFieldValue();
		LOGGER.debug("request list ({})", vo);

		if (vo == null) {
			// nothing to display
			LOGGER.debug("cancelled empty request ({})", vo);
			cancelListRequest();
			return;
		}

		if (vo.equals(requestedVo) && !getItems().isEmpty()) {
			// quick search already done
			LOGGER.debug("cancelled request already ran or running ({})", vo);
			show();
			return;
		}
		requestedVo = vo;

		cancelListRequest();

		final long requestTime = System.nanoTime();
		this.requestTime.set(requestTime);
		ThreadPool.getInstance().submit(new Runnable() {
			@Override
			public void run() {
				try {
					requestList(requestTime, vo.toString());
				} catch (final InterruptedException e) {
					return;
				} catch (final Exception e) {
					LOGGER.error("loading \"{}\"", vo.toString(), e);
					return;
				}
			}
		});
	}

	private void requestList(final long requestId, final String title) throws InterruptedException, MalformedURLException, IOException {
		LOGGER.debug("request list ({}) \"{}\"", requestId, title);

		// allow user to type more characters
		Thread.sleep(500);
		if (requestId != this.requestTime.get()) {
			LOGGER.debug("request list cancelled ({})", requestId);
			return;
		}

		// get a connection
		final String url = String.format(SEARCH_PATTERN, URLEncoder.encode(title, "UTF8"));
		final List<QuickSearchVo> qsResults = new ArrayList<QuickSearchVo>();
		LOGGER.info("request list ({}) \"{}\"", requestId, url);
		final BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
		if (requestId != this.requestTime.get()) {
			LOGGER.debug("request list cancelled ({})", requestId);
			return;
		}

		// read / parse json data
		final JsonArray jsonQsResults = new JsonParser().parse(reader).getAsJsonArray();
		for (final JsonElement jsonElement : jsonQsResults) {
			qsResults.add(new QuickSearchVo(jsonElement.getAsJsonObject()));
		}
		if (requestId != this.requestTime.get()) {
			LOGGER.debug("request list cancelled ({})", requestId);
			return;
		}

		// launch GUI update
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (requestId != AllocineField.this.requestTime.get()) {
					LOGGER.debug("request list cancelled ({})", requestId);
					return;
				}
				getItems().setAll(qsResults);
				if (!getItems().isEmpty()) {
					show();
				}
				LOGGER.info("request list displayed ({}) \"{}\"", requestId, url);
			}
		});
	}

	private void cancelListRequest() {
		LOGGER.debug("cancel quick search");
		requestTime.set(0);
	}
}
