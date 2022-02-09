# Holodeck SMP Server
Holodeck SMP Server is a _service meta-data publisher_ that provides _capability_ information of _Participants_ in a data exchange network. It is commonly used in four-corner networks. 

The current version supports the OASIS Standard [Service Metadata Publishing (SMP) version 2.0](https://docs.oasis-open.org/bdxr/bdx-smp/v2.0/os/bdx-smp-v2.0-os.html). It has a web-based user interface for the administration of the participant's capabilities.

**NOTE:** This is a pre-release version suitable for use in test environments. As it still under development, it is not recommended for use in production environments yet. 

__________________
Lead developer: Sander Fieten
Code hosted at https://github.com/holodeck-b2b/bdxr-smp-server
Issue tracker https://github.com/holodeck-b2b/bdxr-smp-server/issues

## Deployment
To run the Holodeck SMP Server you will need a Java 16 run-time environment. The application itself consists of a single executable jar file. It can be ran out of the box in which case it will use the default configuration that will make the SMP server available for querying on port 80, the administration UI on port 8080, stores the meta-data in a file in the current directory and logs to the console. 

### Configuration
#### Working directory
When run out of the box the SMP server will use the current directory as its working directory where it will store its data and expects any configuration files. To specify the working directory set the Java system property `smp.home` to the path of the directory to use by adding the `-Dsmp.home=«path to working dir»` command line argument when starting the server.  

#### HTTPS
Both the servers for querying the SMP server and the administration UI can be secured using HTTPS. To set up the secure connections a configuration file must be created in the server's working directory, `query-api.properties` to configure the "query server" and `admin-ui.properties` for the administration UI. These are Java properties files which must contain the following properties to setup the HTTPS connections:

- `server.port`: the port on which the server is listening.
- `server.ssl.key-store`: the path to the key store that contains the TLS key and certificate. You can use _${smp.home}_ to specify the SMP's working directory.
- `server.ssl.key-store-password`: the password used to access the key store.
- `server.ssl.key-store-type`: the type of the key store (JKS or PKCS12).
- `server.ssl.key-alias`: the alias that identifies the key in the key store.
- `server.ssl.key-password`: the password used to access the key in the key store.

## Usage



## Contributing
We are using the simplified Github workflow to accept modifications which means you should:
* create an issue related to the problem you want to fix or the function you want to add (good for traceability and cross-reference)
* fork the repository
* create a branch (optionally with the reference to the issue in the name)
* write your code
* commit incrementally with readable and detailed commit messages
* submit a pull-request against the master branch of this repository

If your contribution is more than a patch, please contact us beforehand to discuss which branch you can best submit the pull request to.

### Submitting bugs
You can report issues directly on the [project Issue Tracker](https://github.com/holodeck-b2b/bdxr-smp-server/issues).
Please document the steps to reproduce your problem in as much detail as you can (if needed and possible include screenshots).

## Versioning
Version numbering follows the [Semantic versioning](http://semver.org/) approach.

## Licence
This software is licensed under the Affero General Public License V3 (AGPLv3) which is included in the [LICENSE](LICENSE) file in the root of the project.

## Support
Commercial support is provided by Chasquis Consulting. Visit [Chasquis-Consulting.com](http://chasquis-consulting.com/holodeck-b2b-support/) for more information.
