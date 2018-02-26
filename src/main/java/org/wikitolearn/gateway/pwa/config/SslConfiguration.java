package org.wikitolearn.gateway.pwa.config;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.commons.httpclient.DefaultApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import lombok.extern.slf4j.Slf4j;

/**
 * Custom SSL connection for Zuul.
 * This is a workaround that should be fixed in the next releases.
 * Referenced issues are: #2498, #2503, #2689
 */
@Configuration
@Slf4j
public class SslConfiguration extends DefaultApacheHttpClientConnectionManagerFactory {

  @Autowired
  private ZuulProperties zuulProperties;

  @Value("${server.ssl.trust-store}")
  private Resource trustStore;
  @Value("${server.ssl.trust-store-password}")
  private String trustStorePassword;
  @Value("${server.ssl.key-store}")
  private Resource keyStore;
  @Value("${server.ssl.key-store-password}")
  private String keyStorePassword;
  @Value("${server.ssl.key-password}")
  private String keyPassword;
  @Value("${server.ssl.key-alias}")
  private String keyAlias;
  @Value("${server.ssl.enabled}")
  private boolean sslEnabled;

  @Override
  public HttpClientConnectionManager newConnectionManager(boolean disableSslValidation, int maxTotalConnections,
      int maxConnectionsPerRoute, long timeToLive, TimeUnit timeUnit, RegistryBuilder registryBuilder) {

    if (registryBuilder == null) {
      registryBuilder = RegistryBuilder.<ConnectionSocketFactory>create().register(HTTP_SCHEME,
          PlainConnectionSocketFactory.INSTANCE);
    }

    SSLContext sslContext;
    try {
      SSLContextBuilder sslContextBuilder = new SSLContextBuilder().loadKeyMaterial(keyStore.getURL(),
          keyStorePassword.toCharArray(), keyPassword.toCharArray(), (aliases, socket) -> keyAlias);
      if (disableSslValidation) {
        sslContextBuilder.loadTrustMaterial((x509Certificates, s) -> true);
      } else {
        sslContextBuilder.loadTrustMaterial(trustStore.getURL(), trustStorePassword.toCharArray());
      }
      sslContext = sslContextBuilder.build();
    } catch (NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyStoreException
        | IOException | KeyManagementException e) {
      log.error("Error creating SSLContext", e);
      throw new RuntimeException(e);
    }
    
    SSLConnectionSocketFactory socketFactory;
    
    if (zuulProperties.isSslHostnameValidationEnabled()) {
      socketFactory = new SSLConnectionSocketFactory(sslContext);
    } else {
      socketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
    }

    registryBuilder.register(HTTPS_SCHEME, socketFactory);

    final Registry<ConnectionSocketFactory> registry = registryBuilder.build();

    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry, null, null,
        null, timeToLive, timeUnit);
    connectionManager.setMaxTotal(maxTotalConnections);
    connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);

    return connectionManager;
  }

  @Bean
  @ConditionalOnMissingBean(HttpClientConnectionManager.class)
  public HttpClientConnectionManager connectionManager(
      ApacheHttpClientConnectionManagerFactory connectionManagerFactory, ZuulProperties zuulProperties) {
    ZuulProperties.Host hostProperties = zuulProperties.getHost();
    final HttpClientConnectionManager connectionManager = connectionManagerFactory.newConnectionManager(false,
        hostProperties.getMaxTotalConnections(), hostProperties.getMaxPerRouteConnections(),
        hostProperties.getTimeToLive(), hostProperties.getTimeUnit(), null);
    return connectionManager;
  }

  @Bean
  @ConditionalOnMissingBean
  public ApacheHttpClientConnectionManagerFactory connManFactory() {
    return new SslConfiguration();
  }
}