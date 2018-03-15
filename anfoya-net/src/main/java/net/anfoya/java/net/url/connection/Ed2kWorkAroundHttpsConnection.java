package net.anfoya.java.net.url.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

import net.anfoya.java.io.Ed2kWorkaroundInputStream;

public class Ed2kWorkAroundHttpsConnection extends HttpsURLConnection {
	private final HttpsURLConnection delegate;
	private InputStream inputStream;

	public Ed2kWorkAroundHttpsConnection(final HttpsURLConnection connection) throws IOException {
		super(connection.getURL());
		delegate = connection;
	}

	@Override
	public void connect() throws IOException {
		delegate.connect();
	}

	@Override
	public InputStream getInputStream() throws MalformedURLException, IOException {
		if (inputStream == null) {
			inputStream = delegate.getInputStream();
			if (delegate.getContentType() != null && delegate.getContentType().contains("text")) {
				inputStream = new Ed2kWorkaroundInputStream(inputStream);
			}
		}
		return inputStream;
	}

	@Override
	public String getHeaderFieldKey(final int n) {
		return delegate.getHeaderFieldKey(n);
	}

	@Override
	public boolean equals(final Object obj) {
		return delegate.equals(obj);
	}

	@Override
	public String getHeaderField(final int n) {
		return delegate.getHeaderField(n);
	}

	@Override
	public boolean getInstanceFollowRedirects() {
		return delegate.getInstanceFollowRedirects();
	}

	@Override
	public int getConnectTimeout() {
		return delegate.getConnectTimeout();
	}

	@Override
	public int getContentLength() {
		return delegate.getContentLength();
	}

	@Override
	public long getContentLengthLong() {
		return delegate.getContentLengthLong();
	}

	@Override
	public String getContentType() {
		return delegate.getContentType();
	}

	@Override
	public String getContentEncoding() {
		return delegate.getContentEncoding();
	}

	@Override
	public long getExpiration() {
		return delegate.getExpiration();
	}

	@Override
	public long getDate() {
		return delegate.getDate();
	}

	@Override
	public long getHeaderFieldDate(final String name, final long Default) {
		return delegate.getHeaderFieldDate(name, Default);
	}

	@Override
	public long getLastModified() {
		return delegate.getLastModified();
	}

	@Override
	public void disconnect() {
		delegate.disconnect();
	}

	@Override
	public String getHeaderField(final String name) {
		return delegate.getHeaderField(name);
	}

	@Override
	public Map<String, List<String>> getHeaderFields() {
		return delegate.getHeaderFields();
	}

	@Override
	public int getHeaderFieldInt(final String name, final int Default) {
		return delegate.getHeaderFieldInt(name, Default);
	}

	@Override
	public InputStream getErrorStream() {
		return delegate.getErrorStream();
	}

	@Override
	public long getHeaderFieldLong(final String name, final long Default) {
		return delegate.getHeaderFieldLong(name, Default);
	}

	@Override
	public Object getContent() throws IOException {
		return delegate.getContent();
	}

	@Override
	public Object getContent(@SuppressWarnings("rawtypes") final Class[] classes) throws IOException {
		return delegate.getContent(classes);
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return delegate.getOutputStream();
	}

	@Override
	public boolean getDoInput() {
		return delegate.getDoInput();
	}

	@Override
	public boolean getDoOutput() {
		return delegate.getDoOutput();
	}

	@Override
	public boolean getAllowUserInteraction() {
		return delegate.getAllowUserInteraction();
	}

	@Override
	public void addRequestProperty(final String key, final String value) {
		delegate.addRequestProperty(key, value);
	}

	@Override
	public String getCipherSuite() {
		return delegate.getCipherSuite();
	}

	@Override
	public boolean getDefaultUseCaches() {
		return delegate.getDefaultUseCaches();
	}

	@Override
	public HostnameVerifier getHostnameVerifier() {
		return delegate.getHostnameVerifier();
	}

	@Override
	public long getIfModifiedSince() {
		return delegate.getIfModifiedSince();
	}

	@Override
	public Certificate[] getLocalCertificates() {
		return delegate.getLocalCertificates();
	}

	@Override
	public Principal getLocalPrincipal() {
		return delegate.getLocalPrincipal();
	}

	@Override
	public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
		return delegate.getPeerPrincipal();
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public void setFixedLengthStreamingMode(final int contentLength) {
		delegate.setFixedLengthStreamingMode(contentLength);
	}

	@Override
	public void setFixedLengthStreamingMode(final long contentLength) {
		delegate.setFixedLengthStreamingMode(contentLength);
	}

	@Override
	public void setChunkedStreamingMode(final int chunklen) {
		delegate.setChunkedStreamingMode(chunklen);
	}

	@Override
	public void setInstanceFollowRedirects(final boolean followRedirects) {
		delegate.setInstanceFollowRedirects(followRedirects);
	}

	@Override
	public void setConnectTimeout(final int timeout) {
		delegate.setConnectTimeout(timeout);
	}

	@Override
	public void setRequestMethod(final String method) throws ProtocolException {
		delegate.setRequestMethod(method);
	}

	@Override
	public void setReadTimeout(final int timeout) {
		delegate.setReadTimeout(timeout);
	}

	@Override
	public int getReadTimeout() {
		return delegate.getReadTimeout();
	}

	@Override
	public String getRequestMethod() {
		return delegate.getRequestMethod();
	}

	@Override
	public int getResponseCode() throws IOException {
		return delegate.getResponseCode();
	}

	@Override
	public URL getURL() {
		return delegate.getURL();
	}

	@Override
	public String getResponseMessage() throws IOException {
		return delegate.getResponseMessage();
	}

	@Override
	public boolean usingProxy() {
		return delegate.usingProxy();
	}

	@Override
	public Permission getPermission() throws IOException {
		return delegate.getPermission();
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	@Override
	public void setDoInput(final boolean doinput) {
		delegate.setDoInput(doinput);
	}

	@Override
	public void setDoOutput(final boolean dooutput) {
		delegate.setDoOutput(dooutput);
	}

	@Override
	public void setAllowUserInteraction(final boolean allowuserinteraction) {
		delegate.setAllowUserInteraction(allowuserinteraction);
	}

	@Override
	public void setUseCaches(final boolean usecaches) {
		delegate.setUseCaches(usecaches);
	}

	@Override
	public boolean getUseCaches() {
		return delegate.getUseCaches();
	}

	@Override
	public void setIfModifiedSince(final long ifmodifiedsince) {
		delegate.setIfModifiedSince(ifmodifiedsince);
	}

	@Override
	public void setDefaultUseCaches(final boolean defaultusecaches) {
		delegate.setDefaultUseCaches(defaultusecaches);
	}

	@Override
	public String getRequestProperty(final String key) {
		return delegate.getRequestProperty(key);
	}

	@Override
	public Map<String, List<String>> getRequestProperties() {
		return delegate.getRequestProperties();
	}

	@Override
	public SSLSocketFactory getSSLSocketFactory() {
		return delegate.getSSLSocketFactory();
	}

	@Override
	public Certificate[] getServerCertificates()
			throws SSLPeerUnverifiedException {
		return delegate.getServerCertificates();
	}

	@Override
	public void setHostnameVerifier(final HostnameVerifier v) {
		delegate.setHostnameVerifier(v);
	}

	@Override
	public void setRequestProperty(final String key, final String value) {
		delegate.setRequestProperty(key, value);
	}

	@Override
	public void setSSLSocketFactory(final SSLSocketFactory sf) {
		delegate.setSSLSocketFactory(sf);
	}
}
