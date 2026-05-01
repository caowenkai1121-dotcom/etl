package com.etl.datasource.connector;

public interface ConnectorSpi {
    String getType();
    DatabaseConnector create(ConnectorConfig config);
}
