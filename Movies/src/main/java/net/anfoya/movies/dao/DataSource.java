package net.anfoya.movies.dao;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSource extends BasicDataSource {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataSource.class);

	private static final int DEFAULT_POOL_SIZE = 10;

	private static final String PROPERTY_FILENAME = "database.properties";
	private static final String PROPERTY_DRIVER = "DRIVER";
	private static final String PROPERTY_URL = "URL";
	private static final String PROPERTY_POOL_SIZE = "POOL_SIZE";

	public DataSource() throws IOException {
		final Properties properties = new Properties();
		properties.load(getClass().getResourceAsStream(PROPERTY_FILENAME));

		final String driver = properties.getProperty(PROPERTY_DRIVER);
		LOGGER.info("driver = {}", driver);

		final String url = properties.getProperty(PROPERTY_URL);
		LOGGER.info("url = {}", url);

		final int poolSize = Integer.parseInt(properties.getProperty(PROPERTY_POOL_SIZE, "" + DEFAULT_POOL_SIZE));
		LOGGER.info("pool size = {}", poolSize);

		setDriverClassName(driver);
		setUrl(url);
		setInitialSize(poolSize);
	}
}