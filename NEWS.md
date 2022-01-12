## 4.8.0 IN-PROGRESS

## 4.7.0 2022-01-12

* Removed UUID string pattern from RAML file, as module now uses externalSystemID to look up patron data [EDGPATRON-61] (https://issues.folio.org/browse/EDGPATRON-61)
* Include API key parameter for every endpoint in RAML. [EDGPATRON-63](https://issues.folio.org/browse/EDGPATRON-63)
* Update Log4j to 2.17.0. (CVE-2021-44228, CVE-2021-45105) ([EDGPATRON-67](https://issues.folio.org/browse/EDGPATRON-67), [EDGPATRON-74](https://issues.folio.org/browse/EDGPATRON-74))
* [EDGPATRON-64](https://issues.folio.org/browse/EDGPATRON-64): Update interface circulation version to 12.0
* [EDGPATRON-62](https://issues.folio.org/browse/EDGPATRON-62): Support new query arguments:  sortBy, limit, offset for patron account info endpoint
* [EDGPATRON-72](https://issues.folio.org/browse/EDGPATRON-72): Inconsistent handling of invalid limit parameter

## 4.6.0 2021-09-27

* Upgrade to vert.x 4.x [EDGPATRON-39] (https://issues.folio.org/browse/EDGPATRON-39)

## 4.5.0 2021-06-17

* Requires `circulation 9.5 10.0 or 11.0` ([EDGPATRON-44](https://issues.folio.org/browse/EDGPATRON-44), [EDGPATRON-46](https://issues.folio.org/browse/EDGPATRON-46))

## 4.4.0 2021-03-18

* Introduces patron comments on requests (EDGPATRON-33)
* Requires `patron 4.2`
* Required `circulation 9.5`

## 4.3.0 2020-10-20
 * [EDGPATRON-35](https://issues.folio.org/browse/EDGPATRON-35): Upgrade to JDK11

## 4.2.0 2020-06-12
 * [EDGPATRON-30](https://issues.folio.org/browse/EDGPATRON-30): EDGEPATRON (edge-patron) release
 * [EDGPATRON-31](https://issues.folio.org/browse/EDGPATRON-31): Add 7.0 as acceptable login interface version

## 4.1.0 2019-12-08
 * [EDGPATRON-27](https://issues.folio.org/browse/EDGPATRON-27): Cancelling Requests failed by non-UUID patronID

## 4.0.0 2019-12-08
 * [EDGPATRON-20](https://issues.folio.org/browse/EDGPATRON-20): Process request cancellation comming from patron portal in discovery layer
 * [FOLIO-2235](https://issues.folio.org/browse/FOLIO-2235): Add LaunchDescriptor settings to each backend non-core module repository
 * [FOLIO-2358](https://issues.folio.org/browse/FOLIO-2358): Use JVM features (UseContainerSupport, MaxRAMPercentage) to manage container memory

## 3.0.3 2019-09-11
 * [EDGPATRON-19](https://issues.folio.org/browse/EDGPATRON-19): Set request creation date to timestamp and remove expiration date if it's invalid.

## 3.0.2 2019-07-24
 * [EDGPATRON-18](https://issues.folio.org/browse/EDGPATRON-18): Add `login` 6.0 interface

## 3.0.1 2019-07-10
 * [EDGPATRON-17](https://issues.folio.org/browse/EDGPATRON-17): Update to `edge-common` v2.0.2

## 3.0.0 2019-06-10
 * [EDGPATRON-13](https://issues.folio.org/browse/EDGPATRON-13): Update to the `patron` 3.0 interface
 * [EDGPATRON-5](https://issues.folio.org/browse/EDGPATRON-5): Update to RAML 1.0
 * [EDGPATRON-14](https://issues.folio.org/browse/EDGPATRON-14): Remove the "queueLength" JSON field

## 2.0.0 2019-03-22
 * [EDGPATRON-11](https://issues.folio.org/browse/EDGPATRON-11): Update to the `patron` 2.0 interface
 * [EDGPATRON-9](https://issues.folio.org/browse/EDGPATRON-9): Update to `edge-common` v2.0.0
 * [EDGPATRON-7](https://issues.folio.org/browse/EDGPATRON-7): Add `login` 2.0 interface dependency

## 1.0.0 2018-12-05
 * [EDGPATRON-4](https://issues.folio.org/browse/EDGPATRON-4): JSON descriptions

## 0.1.7 Unreleased
 * Initial Commit
