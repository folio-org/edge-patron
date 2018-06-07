# edge-patron

Copyright (C) 2018 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

Edge API to interface with FOLIO for 3rd party discovery services to allow patrons to perform self-service actions (place holds, renewals, etc)

## Overview

The purpose of this edge API is to bridge the gap between 3rd party discovery services and FOLIO.  More specifically, the initial implementation was built with EBSCO's Discovery Service (EDS) in mind and the patron empowerment portion of the RTAC service.  Paton empowerment allows patrons to perform self-service actions such as manage their holds, renewals, etc.

## Security

TBD

## Configuration

Configuration information is specified in two forms:
1. System Properties - General configuration
1. Properties File - Configuration specific to the desired secure store

### System Properties

Proprety              | Default     | Description
--------------------- | ----------- | -------------
`port`                | `8081`      | Server port to listen on
`okapi_url`           | *required*  | Where to find Okapi (URL)
`secure_store`        | `Ephemeral` | Type of secure store to use.  Valid: `Ephemeral`, `AwsSsm`, `Vault`
`secure_store_props`  | `NA`        | Path to a properties file specifying secure store configuration
`token_cache_ttl_ms`  | `3600000`   | How long to cache JWTs, in milliseconds (ms)
`token_cache_capacity`| `100`       | Max token cache size
`log_level`           | `INFO`      | Log4j Log Level
`request_timeout_ms`  | `30000`     | Request Timeout

## Additional information

### Issue tracker

See project [FOLIO](https://issues.folio.org/browse/FOLIO)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)

