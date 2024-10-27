package com.example.learning.multitenant.datasourcesetting;

import lombok.Builder;
import lombok.Setter;

@Setter
// pojo class sử dụng để chứa thông tin data source
public abstract class DataSourceSettings {
  private String serverHost;
  private int serverPort;
  private String databaseName;
  private String requestParameters;
  private String username;
  private String password;
  private Integer minimumIdle;
  private Integer maximumPoolSize;
  private Long idleTimeout;
  private Long maxLifetime;
  private Long connectionTimeout;
  private Long serviceId;

  protected DataSourceSettings() {

  }

  public String getServerHost() {
    return serverHost;
  }

  public int getServerPort() {
    return serverPort;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public String getRequestParameters() {
    return requestParameters != null ? requestParameters : "";
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public Integer getMinimumIdle() {
    return minimumIdle != null ? minimumIdle : 5;
  }

  public Integer getMaximumPoolSize() {
    return maximumPoolSize != null ? maximumPoolSize : 20;
  }

  public Long getIdleTimeout() {
    return idleTimeout != null ? idleTimeout : 5000;
  }

  public Long getMaxLifetime() {
    return maxLifetime != null ? maxLifetime : 300000;
  }

  public Long getConnectionTimeout() {
    return connectionTimeout != null ? connectionTimeout: 30000;
  }

  public Long getServiceId() {
    return serviceId;
  }

  public abstract String getDriverClassName();

  public abstract String getBaseUrl();
}
