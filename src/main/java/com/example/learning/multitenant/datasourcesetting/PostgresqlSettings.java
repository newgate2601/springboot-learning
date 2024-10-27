package com.example.learning.multitenant.datasourcesetting;

import lombok.Builder;
import lombok.NoArgsConstructor;

// config drivenClass and baseUrl for postgreSQL
@Builder
@NoArgsConstructor
public class PostgresqlSettings extends DataSourceSettings {

  @Override
  public String getDriverClassName() {
    return "org.postgresql.Driver";
  }

  @Override
  public String getBaseUrl() {
    return "jdbc:postgresql://" + getServerHost() + ":" + getServerPort() + "/";
  }
}
