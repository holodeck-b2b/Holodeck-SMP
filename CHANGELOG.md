# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## 2.0.0
##### 2024-02-05
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

