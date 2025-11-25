# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## 3.0.2
##### 2025-11-25
### Fixed
* Validation error on schemeId when updating an ID Scheme
* Internal error when trying to edit a meta-data registration with an identifier that contains a '+' character
* Admin role lost when administrator updates own account  

## 3.0.1
##### Unreleased
### Fixed
* Internal server error when relative URL is provided in the SMP server configuration.
* Internal server error when trying to register a new SMP server certificate in the Peppol SML with current date as 
 activation date
* Certificate update not rolled back on SML registration error
* Missing error message when an error occurs registering the new SMP certificate with the SML
* Logging in audit log service 

## 3.0.0
##### Unreleased
### Added 
* Support for TOTP based two factor authentication. Use of 2FA can be configured to be required for all users.
* Locking of users accounts. Accounts will automatically be locked after, a configurable, number of failed login 
  attempts or can be locked by an administrator.
* Audit logging. All actions related to authentication and changes in meta-data are logged in the audit log.
* Search function for Participants to find Participants based on their identifier, business name, SML or Directory 
  registration state.  
* Interface layer that defines the API of the Holodeck SMP Core and can be used to implement extensions

### Changed
* The use of a network SML and Directory service has been generalised and is not limited to Peppol only
* Certificate updates can be scheduled to be applied in the future (was already required when using the SMP server in 
  the Peppol network, but has also been generalised)
* External URL (as registered in SML) can be configured instead of only host name allowing both https and custom context
  paths to be used for queries.

### Fixed
* Slow loading of the Participant overview page when there are many Participants registrations.
* Signature//Reference includes two Transform element instead of one [#16](https://github.com/holodeck-b2b/Holodeck-SMP/issues/16)
 

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

