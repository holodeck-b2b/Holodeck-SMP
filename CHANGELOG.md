# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## 2.1.0
##### 
### Added 
* Support for migration of Participants between Service Providers (Peppol only). Migration is supported in both the manangement UI as well as the REST API.
* Ability to set the first registration date and additional identifiers of a Participant. When the SMP server is integrated with a directory service these are published to the directory as well. These data elements are both supported in the management UI as well as the REST API
* Configuration option in the management API to indicate whether new Participant registrations should automatically be added in the SML as well (if the SMP is registered with an SML)  

### Changed
* Availability of the REST API is now automatically determined and does not require defining the _api.enabled_ configuration property
* Updated to version 3.3.10 of the Spring Boot framework
* Updated to Bootstrap version 5.3.3
* Minimum Java version to run the SMP server is now 17 

## 2.0.3
##### 2025-01-06
### Fixed
* HTTP 404 Error on delete actions [#9](https://github.com/holodeck-b2b/Holodeck-SMP/issues/9)

## 2.0.2
##### 2024-12-19
### Fixed
* Use latest version of Peppol BusinessCard XSD [#4](https://github.com/holodeck-b2b/Holodeck-SMP/issues/4)
* Setting Endpoint activation or expiration date results in invalid Peppol SMP response [#5](https://github.com/holodeck-b2b/Holodeck-SMP/issues/5)
* Activation and expiration dates cannot be removed once set [#6](https://github.com/holodeck-b2b/Holodeck-SMP/issues/6)
* Edit EP cert changes to add on input validation error [#7](https://github.com/holodeck-b2b/Holodeck-SMP/issues/7)
* Activation and expiration dates not stored in UTC [#8](https://github.com/holodeck-b2b/Holodeck-SMP/issues/8)

## 2.0.1
##### 2024-06-13
### Fixed
* Participant not published to Peppol directory [#3](https://github.com/holodeck-b2b/Holodeck-SMP/issues/3)

## 2.0.0
##### 2024-06-07
### Added
* Support for Peppol Directory Business Cards
* Support to automatically register the certificate update in the Peppol SML
* REST API to manage Participants
* Option to select which servers (Query/AdminUI/REST API) are started
* Specific exceptions for errors in communication with SML and directory
* Signing algorithms for Peppol responses is now configurable

### Changed
* Renamed `org.holodeckb2b.bdxr.smp.server.queryapi.QueryUtils`  to `org.holodeckb2b.bdxr.smp.server.svc.IdUtils`
* Default signing algorithms for Peppol responses changed to SHA-256

## 1.0.1
##### 2023-08-24
### Fixed
* Incorrect version of _generic-utils_ test dedepency [#1](https://github.com/holodeck-b2b/Holodeck-SMP/issues/1)
* Case-sensitivity handling of identifiers. When case-sensitivity of an ID-Scheme is changed this is now correctly
  applied to existing records.

## 1.0.0
##### 2023-03-22
### Added
* Initial release.

