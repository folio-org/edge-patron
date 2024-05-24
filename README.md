# edge-patron

Copyright (C) 2018-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

Edge API to interface with FOLIO for 3rd party discovery services to allow patrons to perform self-service actions (place holds, renewals, etc)

## Overview

The purpose of this edge API is to bridge the gap between 3rd party discovery services and FOLIO.  More specifically, the initial implementation was built with EBSCO's Discovery Service (EDS) in mind and the patron empowerment portion of the RTAC service.  Paton empowerment allows patrons to perform self-service actions such as manage their holds, renewals, etc.

## Security

The edge-patron API is secured via the facilities provided by edge-common.  More specifically, via API Key.  See edge-common for additional details.

## Required Permissions

Institutional users should be granted the following permissions in order to use this edge API:
- `patron.all`
- `users.collection.get`
- `circulation.requests.item.get`

## Configuration

Configuration information is specified in two forms:
1. System Properties - General configuration
1. Properties File - Configuration specific to the desired secure store

### System Properties

| Property                      | Default             | Description                                                                |
|-------------------------------|---------------------|----------------------------------------------------------------------------|
| `port`                        | `8081`              | Server port to listen on                                                   |
| `okapi_url`                   | *required*          | Where to find Okapi (URL)                                                  |
| `secure_store`                | `Ephemeral`         | Type of secure store to use.  Valid: `Ephemeral`, `AwsSsm`, `Vault`        |
| `secure_store_props`          | `NA`                | Path to a properties file specifying secure store configuration            |
| `token_cache_ttl_ms`          | `3600000`           | How long to cache JWTs, in milliseconds (ms)                               |
| `null_token_cache_ttl_ms`     | `30000`             | How long to cache login failures (null JWTs), in milliseconds (ms)         |
| `token_cache_capacity`        | `100`               | Max token cache size                                                       |
| `patron_id_cache_ttl_ms`      | `3600000`           | How long to cache patron ID mappings in milliseconds (ms)                  |
| `null_patron_id_cache_ttl_ms` | `30000`             | How long to cache patron lookup failures in milliseconds (ms)              |
| `patron_id_cache_capacity`    | `1000`              | Max token cache size                                                       |
| `log_level`                   | `INFO`              | Log4j Log Level                                                            |
| `request_timeout_ms`          | `30000`             | Request Timeout                                                            |

### Env variables for TLS configuration for Http server
To configure Transport Layer Security (TLS) for the HTTP server in an edge module, the following configuration parameters should be used.
Parameters marked as Required are required only in case when ssl_enabled is set to true.

| Property                                             | Default           | Description                                                                                 |
|------------------------------------------------------|-------------------|---------------------------------------------------------------------------------------------|
| `SPRING_SSL_BUNDLE_JKS_WEB-SERVER_KEYSTORE_TYPE`     | `NA`              | (Required). Set the type of the keystore. Common types include `JKS`, `PKCS12`, and `BCFKS` |
| `SPRING_SSL_BUNDLE_JKS_WEB-SERVER_KEYSTORE_PATH`     | `NA`              | (Required). Set the location of the keystore file in the local file system                  |
| `SPRING_SSL_BUNDLE_JKS_WEB-SERVER_KEYSTORE_PASSWORD` | `NA`              | (Required). Set the password for the keystore                                               |

### Env variables for TLS configuration for Web Client
To configure Transport Layer Security (TLS) for Web clients in the edge module, you can use the following configuration parameters.
Truststore parameters for configuring Web clients are optional even when ssl_enabled = true.
If truststore parameters need to be populated, truststore_type, truststore_path and truststore_password are required.

| Property                                | Default           | Description                                                                      |
|-----------------------------------------|-------------------|----------------------------------------------------------------------------------|
| `FOLIO_CLIENT_TLS_ENABLED`              | `false`           | Set whether SSL/TLS is enabled for Vertx Http Server                             |
| `FOLIO_CLIENT_TLS_TRUST_STORE_TYPE`     | `NA`              | Set the type of the keystore. Common types include `JKS`, `PKCS12`, and `BCFKS`  |
| `FOLIO_CLIENT_TLS_TRUST_STORE_PATH`     | `NA`              | Set the location of the keystore file in the local file system                   |
| `FOLIO_CLIENT_TLS_TRUST_STORE_PASSWORD` | `NA`              | Set the password for the keystore                                                |


## Patron Mapping

In order to map external patron IDs to those used within FOLIO, the `externalSystemId` field in the user metadata is used.  The mapping flow works like this:

1. A request arrives containing an external system's patron ID
1. The patron ID cache is consulted.  If a mapping has been cached, skip to #5.
1. A request is made to mod-users, querying for the user having the provided `externalSystemId`
1. The external ID -> internal/FOLIO ID mapping is cached for a configurable amount of time.
1. The internal/FOLIO ID is used when calling mod-patron

## Additional information

### Issue tracker

See project [EDGPATRON](https://issues.folio.org/browse/EDGPATRON)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)

