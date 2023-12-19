# Holodeck SMP Server
Holodeck SMP Server is a _service meta-data publisher_ that provides _capability_ information of _Participants_ in a data exchange network. It is commonly used in four-corner networks, such as [Peppol](https://peppol.org) and the [Business Payments Coalition’s (BPC) E-invoice Exchange Market Pilot](https://businesspaymentscoalition.org/) to find the _Access Point_ of the _Service Provider_ that handles the exchange of documents, like invoices and orders, on behalf of the _Participant_.
The current version supports the OASIS Standard [Service Metadata Publishing (SMP) version 2.0](https://docs.oasis-open.org/bdxr/bdx-smp/v2.0/os/bdx-smp-v2.0-os.html) as used by the Business Payments Coalation (BPC) and the [Peppol specification](https://docs.peppol.eu/edelivery/smp/PEPPOL-EDN-Service-Metadata-Publishing-1.2.0-2021-02-24.pdf). It  also supports basic integration with the Peppol SML and Peppol Directory.

The server application available for download contains two separated interfaces, a REST based API for responding to queries and a web-based user interface for managing of the Participant's capabilities. By separating the query and management interfaces it is easy to manage access to them independently. Optionally a REST API for managing Participants can be added.

__________________
Lead developer: Sander Fieten  
Code hosted at https://github.com/holodeck-b2b/Holodeck-SMP  
Issue tracker https://github.com/holodeck-b2b/Holodeck-SMP/issues  

## Usage
For more information about installing and using the server, please see the documentation at [our website](https://holodeck-smp.org/).

## Contributing
We are still working on improvements and addition of features. This may result in changes to the internal architecture of the server. If you would like to contribute to this project please contact us, so we ensure efficient cooperation.

### Submitting bugs
You can report issues directly on the [project Issue Tracker](https://github.com/holodeck-b2b/Holodeck-SMP/issues).
Please document the steps to reproduce your problem in as much detail as you can (if needed and possible include screenshots).

## Versioning
Version numbering follows the [Semantic versioning](http://semver.org/) approach.

## Licence
This software is licensed under the Affero General Public License V3 (AGPLv3) which is included in the [LICENSE](LICENSE) file in the root of the project.

## Support
Commercial support is provided by Chasquis. Visit [Chasquis-Consulting.com](http://chasquis-consulting.com/holodeck-b2b-support/) for more information.
