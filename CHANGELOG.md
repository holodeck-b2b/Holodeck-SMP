# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).


## 2.0.2
##### 
### Fixed
* Setting Endpoint activation or expiration date results in invalid Peppol SMP response [#5](https://github.com/holodeck-b2b/Holodeck-SMP/issues/5)
* Activation and expiration dates cannot be removed once set [#6](https://github.com/holodeck-b2b/Holodeck-SMP/issues/6)
* Edit EP cert changes to add on input validation error [#7](https://github.com/holodeck-b2b/Holodeck-SMP/issues/7)
* Activation and expiration dates not stored in UTC [#8](https://github.com/holodeck-b2b/Holodeck-SMP/issues/8)

## 2.0.1
##### 2024-06-13
### Fixed
* Participant not published to Peppol directory [#2](https://github.com/holodeck-b2b/Holodeck-SMP/issues/2)

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

