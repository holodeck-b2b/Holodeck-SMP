## REST Management API
This module contains a REST API for managing the Participant served by the SMP server. With this API a back-end application can add, update and delete Participants and manage the bindings of Service Metadata Templates. The scope of the API is limited to the Participant data as the other  meta-data, like the supported Services and Processes and used Endpoints are much more static. 

### Installation and configuration
To add the management API to the SMP server the `mgmt-api-«version».jar` must be copied to the SMP deployment directory and the server must be started using the following command: `java -Dloader.path=mgmt-api-«version».jar -jar holodeck-smp-server-«version».jar`. The management API will then be available on port 8585. To change the port the API is listening on add a properties file named `mgmt-api.properties` to the SMP deployment directory and set the _server.port_ property.

When the SMP server is registered in the SML, the Participant registration function of the management API by default will automatically register new participants in the SML (or migrate the Participant when a migration code has been provided). This means that the new Participant will not have any services published until they are registered through the Manage Service Bindings API. This automatic registration can be disabled by setting the _sml.autoregistration_ property with value _false_ in the `mgmt-api.properties` configuration file.

### API Specification
#### Preconditions
As the API can only be used to manage the manage the Participant registration and the binding of Service Metadata Templates to Participants the Service Metadata Templates must already be configured in the web interface before the API can be effectively used. 
If Participants and their associated business info should be registered in the SML and directory (currently applies only to Peppol) the SMP Server must already be registered in the SML. 

#### Managing Participants
Participant registrations are managed using the `/participants` resource. 
A new Participant registration is added by executing a PUT request with the Participant Identifier added to the URL, i.e. `/participants/«ParticipantID»`. The identifier should be in the URL encoded format specified in [section 3.6.3](https://docs.oasis-open.org/bdxr/bdx-smp/v2.0/os/bdx-smp-v2.0-os.html#_Toc62566898) of the OASIS SMP Version 2.0 specification. When automatic SML registratio is enabled, an optional query parameter `migrationCode` may be added to provide the _migration code_ when the Participant is moved from another SMP to this SMP, e.g. `/participants/«ParticipantID»?migrationCode=«migration code»`.
The server will respond with following HTTP response codes to indicate how the request was processed:

| HTTP status                 | Indicates      |
| :-------------------------- | :------------- | 
| 201 (Created)               | The Participant Identifier was successfully registered and if SML integration is enabled added to the SML | 
| 400 (Bad Request)           | The specified Participant Identifier could not be parsed or the specified scheme does not exist |
| 409 (Conflict)              | The specified Participant Identifier already exists |
| 424 (Failed dependency)     | The Participant Identifier was successfully registered, but could not be added to the SML |
| 500 (Internal Server Error) | An unexpected error occurred during the processing of the request | 

To delete a Participant registration a DELETE request with the Participant Identifier added to the URL, i.e. `/participants/«ParticipantID»` should be executed. NOTE that when there are existing bindings to Service Metadata Templates, these are removed. The server indicates the result of the deletion request using the following HTTP status codes:

| HTTP status                 | Indicates      |
| :-------------------------- | :------------- | 
| 202 (Accepted)              | The Participant was successfully deleted and if applicable removed from the SML and directory |
| 424 (Failed dependency)     | An error occurred in removing the Participant from the SML or directory which prevents the Participant from being removed from the SMP | 
| 400 (Bad Request)           | The specified Participant Identifier could not be parsed or the specified scheme does not exist |
| 500 (Internal Server Error) | An unexpected error occurred during the processing of the request | 

##### SML Registration
The registration of a Participant in the SML can be managed using the `/participants/«ParticipantID»/sml` resource where ParticipantID should be in the URL  encoded format specified in [section 3.6.3](https://docs.oasis-open.org/bdxr/bdx-smp/v2.0/os/bdx-smp-v2.0-os.html#_Toc62566898) of the OASIS SMP Version 2.0 specification.  
Using the PUT method the Participant is registered in the SML. An optional query parameter `migrationCode` may be added to provide the _migration code_ when the Participant is moved from another SMP to this SMP, e.g. `/participants/«ParticipantID»/sml?migrationCode=«migration code»`.  
Removing the Participant from the SML is done by executing a DELETE request on the resource. Note that deleting the Participant from the SML will also remove it from the directory if it was published to it (as SML registration is a precondition for publication). 
The server indicates the result of the registratio or deletion request using the following HTTP status codes:

| HTTP status                 | Indicates      |
| :-------------------------- | :------------- | 
| 201 (Created)               | The Participant Identifier was successfully registered in the SML | 
| 202 (Accepted)              | The Participant Identifier was successfully removed from the SML | 
| 400 (Bad Request)           | The specified Participant Identifier could not be parsed or the specified scheme does not exist |
| 404 (Not Found)             | There is no Participant registered with the specified identifier |
| 412 (Precondition failed)   | The Participant is being migrated to another SMP and therefore cannot be removed from the SML |
| 424 (Failed dependency)     | The Participant could not be added to or removed from the SML or directory |
| 500 (Internal Server Error) | An unexpected error occurred during the processing of the request | 

Also the migration of a Participant to another SMP can be managed through the Management API. To prepare the migration of a Participant and get the migration code to provide to the other SMP's Service Provider a GET request on the URL `/participants/«ParticipantID»/sml/prepareMigration` should be executed. When the Participant can be migrated, the server will generate a migration code and include it in the response entity body. The HTTP response code will be 200 (OK). Other status codes can be:

| HTTP status                 | Indicates      |
| :-------------------------- | :------------- | 
| 400 (Bad Request)           | The specified Participant Identifier could not be parsed or the specified scheme does not exist |
| 404 (Not Found)             | There is no Participant registered with the specified identifier |
| 424 (Failed dependency)     | The migration code could not be registered in the SML |
| 500 (Internal Server Error) | An unexpected error occurred during the processing of the request | 

If an error occurs when trying to manage the SML registration of the Participant through the API, it is recommended to retry the operation using the web UI to get more details on the error.


#### Managing Service Bindings
The bindings of Service Metadata Templates to a Participant are managed using the `/participants/«ParticipantID»/bindings` resource.   

Bindings are managed using an identifier that is generated for each registered Service Metadata Template. This identifier can be found in the UI on the overview of Service Metadata Templates and on the edit page of the individual template. To facilitate easy selection of services in the back-end system however the API also has the option to retrieve a summary of the registered Service Metadata Templates by executing a GET request of the `/templates` resource. The response will be an XML document as specified by the XML Schema _http://holodeck-b2b.org/schemas/2023/12/bdxr/smp/smt_ which can be found in [/src/main/xsd/smt.xsd](/src/main/xsd/smt.xsd).

To retrieve a summary of all Service Metadata Templates that are bound to a Participant a GET request can be executed to the resource. When a Participant is registered with the given Identifier, the server will respond with an XML document that contains the list of templates bound to that Participant. The structure of the XML document is specified in the XML Schema _http://holodeck-b2b.org/schemas/2023/12/bdxr/smp/smt_ which can be found in [/src/main/xsd/smt.xsd](/src/main/xsd/smt.xsd).

Binding a Service Metadata Template to a Participant is done by executing a PUT request for `/participants/«ParticipantID»/bindings/«template_id»`. Again the Participant Identifier must be formatted as specified in the OASIS SMP specification. As the template id is a simple integer there is no need for encoding. 

| HTTP status       | Indicates      |
| :---------------- | :------------- | 
| 201 (Created)     | The Service Metadata Template was succesfully bound to the Participant | 
| 400 (Bad Request) | The specified Participant Identifier could not be parsed or the specified scheme does not exist |
| 404 (Not Found)   | Either no Participant or Service Metadata Template with the specified identifier could be found |
| 500 (Internal Server Error) | An unexpected error occurred during the processing of the request |

Removing a binding between Service Metadata Template and Participant is done by a DELETE request for `/participants/«ParticipantID»/bindings/«template_id»`. The HTTP response codes are:

| HTTP status       | Indicates      |
| :---------------- | :------------- | 
| 202 (Accepted)    | The binding between Service Metadata Template and Participant was succesfully removed | 
| 400 (Bad Request) | The specified Participant Identifier could not be parsed or the specified scheme does not exist. |
| 404 (Not Found)   | No Participant with the specified identifier could be found. |
| 500 (Internal Server Error) | An unexpected error occurred during the processing of the request. |

#### Managing Business Cards
The information about the business entity to be included in the Business Card of a Participant and published in the Directory is managed through the `/participants/«ParticipantID»/businesscard` resource. Currently only one business entity can be added to the Business Card.  
The information is created or updated by executing a PUT request on the resource which should contain an XML document with the business information in the HTTP entity body. The root element of the XML document must be a `BusinessEntity` element as specified in the XML Schema used by the Peppol Directory: _[http://www.peppol.eu/schema/pd/businesscard/20161123/](../peppol-smp/src/main/xsd/peppol-directory-business-card-20161123.xsd)_. The server repsonds with the following HTTP status codes to indicate the processing result:

| HTTP status           | Indicates      |
| :-------------------- | :------------- | 
| 202 (Accepted)        | The Business Card of the Participant was succesfully updated and published to the Directory | 
| 206 (Partial Content) | The Business Card of the Participant was succesfully updated, but there was an error publishing to the Directory. When this happens publishing can be retried by executing the update again either through the API or UI | 
| 400 (Bad Request)     | The provided data is invalid. It could be that the specified Participant Identifier could not be parsed or its specified scheme does not exist or required business entity information is missing |
| 404 (Not Found)       | No Participant with the specified identifier could be found |
| 500 (Internal Server Error) | An unexpected error occurred during the processing of the request |

Removing the Bussiness Card from a Participant can be done by executing a DELETE request on the resource. The following HTTP response status codes are used to indicate the processing result:

| HTTP status                 | Indicates      |
| :-------------------------- | :------------- | 
| 202 (Accepted)              | The Business Card was successfully deleted and if applicable removed from the directory |
| 404 (Not Found)             | No Participant with the specified identifier could be found |
| 424 (Failed dependency)     | An error occurred removing the Business Card from the directory which prevents the action to be completed. When this happens the request should be retried | 
| 400 (Bad Request)           | The specified Participant Identifier could not be parsed or the specified scheme does not exist |
| 500 (Internal Server Error) | An unexpected error occurred during the processing of the request | 

### Licence
This software is licensed under the Affero General Public License V3 (AGPLv3) which is included in the [LICENSE](LICENSE) file in the root of the project.

### Support
Commercial support is provided by Chasquis. Visit [Chasquis-Consulting.com](http://chasquis-consulting.com/holodeck-b2b-support/) for more information.
