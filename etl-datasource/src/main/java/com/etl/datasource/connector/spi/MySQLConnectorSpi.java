package com.etl.datasource.connector.spi;

import com.etl.datasource.connector.ConnectorConfig;
import com.etl.datasource.connector.ConnectorSpi;
import com.etl.datasource.connector.DatabaseConnector;
import com.etl.datasource.connector.MySQLConnector;

public class MySQLConnectorSpi implements ConnectorSpi {
    @Override
    public String getType() {
        return "MYSQL";
    }

    @Override
    public DatabaseConnector create(ConnectorConfig config) {
        return new MySQLConnector(
            config.getDatasourceId(),
            config.getHost(),
            config.getPort(),
            config.getDatabaseName(),
            config.getUsername(),
            config.getPassword(),
            config.getCharset(),
            config.getExtraConfig()
        );
    }
}
