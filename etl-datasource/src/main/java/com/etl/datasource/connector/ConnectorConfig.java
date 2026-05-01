package com.etl.datasource.connector;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectorConfig {
    private Long datasourceId;
    private String type;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String password;
    private String charset;
    private String extraConfig;
}
