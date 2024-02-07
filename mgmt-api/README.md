## REST Management API
This module contains a REST API for managing the Participant served by the SMP server. With this API a back-end application can add, update and delete Participants and manage the bindings of Service Metadata Templates. The scope of the API is limited to the Participant data as the other  meta-data, like the supported Services and Processes and used Endpoints are much more static. 

### Installation and configuration
To add the management API to the SMP server the `mgmt-api-«version».jar` must be copied to the SMP deployment directory and the server must be started using the following command: `java -Dloader.path=mgmt-api-«version».jar -jar holodeck-smp-server-«version».jar`. The management API will then be available on port 8585. To change the port the API is listening on add a properties file named `mgmt-api.properties` to the SMP deployment directory and set the _server.port_ property.

### API Specification
#### Preconditions
As the API can only be used to manage the manage the Participant registration and the binding of Service Metadata Templates to Participants the Service Metadata Templates must already be configured in the web interface before the API can be effectively used. 
If Participants and their associated business info should be registered in the SML and directory (currently applies only to Peppol) the SMP Server must already be registered in the SML. 

#### Managing Participants
Participant registrations are managed using the `/participants` resource. 
A new Participant registration is added by executing a PUT request with the Participant Identifier added to the URL, i.e. `/participants/«PartID»`. The identifier should be in URL encoded format specified in [section 3.6.3](https://docs.oasis-open.org/bdxr/bdx-smp/v2.0/os/bdx-smp-v2.0-os.html#_Toc62566898) of the OASIS SMP Version 2.0 specification.
The server will respond with following HTTP response codes to indicate how the request was processed:

| HTTP status                 | Indicates      |
| :-------------------------- | :------------- | 
| 201 (Created)               | The Participant Identifier was successfully registered and if SML integration is enabled added to the SML | 
| 409 (Conflict)              | The specified Participant Identifier already exists |
| 400 (Bad Request)           | The specified Participant Identifier could not be parsed or the specified scheme does not exist |
| 424 (Failed dependency)     | The Participant Identifier was successfully registered, but could not be added to the SML. When this happens, use the UI to retry registration in the SML.|
| 500 (Internal Server Error) | An unexpected error occurred during the processing of the request | 

To delete a Participant registration a DELETE request with the Participant Identifier added to the URL, i.e. `/participants/«PartID»` should be executed. NOTE that when there are existing bindings to Service Metadata Templates, these are removed. The server indicates the result of the deletion request using the following HTTP status codes:

| HTTP status                 | Indicates      |
| :-------------------------- | :------------- | 
| 202 (Accepted)              | The Participant was successfully deleted and if applicable removed from the SML and directory |
| 424 (Failed dependency)     | An error occurredin removing the Participant from the SML or directory which prevents the Participant from being removed from the SMP | 
| 400 (Bad Request)           | The specified Participant Identifier could not be parsed or the specified scheme does not exist |
| 500 (Internal Server Error) | An unexpected error occurred during the processing of the request | 

#### Managing Service Bindings
The bindings of Service Metadata Templates to a Participant are managed using the `/participants/«ParticipentID»/bindings` resource.   

Bindings are managed using an identifier that is generated for each registered Service Metadata Template. This identifier can be found in the UI on the overview of Service Metadata Templates and on the edit page of the individual template. To facilitate easy selection of services in the back-end system however the API also has the option to retrieve a summary of the registered Service Metadata Templates by executing a GET request of the `/templates` resource. The response will be an XML document as specified by the XML Schema _http://holodeck-b2b.org/schemas/2023/12/bdxr/smp/smt_ which can be found in [/src/main/xsd/smt.xsd](/src/main/xsd/smt.xsd).

To retrieve a summary of all Service Metadata Templates that are bound to a Participant a GET request can be executed to the resource. When a Participant is registered with the given Identifier, the server will respond with an XML document that contains the list of templates bound to that Participant. The structure of the XML document is specified in the XML Schema _http://holodeck-b2b.org/schemas/2023/12/bdxr/smp/smt_ which can be found in [/src/main/xsd/smt.xsd](/src/main/xsd/smt.xsd).

Binding a Service Metadata Template to a Participant is done by executing a PUT request for `/participants/«ParticipentID»/bindings/«template_id»`. Again the Participant Identifier must be formatted as specified in the OASIS SMP specification. As the template id is just a simple long there is no need for encoding. 

| HTTP status       | Indicates      |
| :---------------- | :------------- | 
| 201 (Created)     | The Service Metadata Template was succesfully bound to the Participant | 
| 400 (Bad Request) | The specified Participant Identifier could not be parsed or the specified scheme does not exist |
| 404 (Not Found)   | Either no Participant or Service Metadata Template with the specified identifier could be found |
| 500 (Internal Server Error) | An unexpected error occurred during the processing of the request |

Removing a binding between Service Metadata Template and Participant is done by a DELETE request for `/participants/«ParticipentID»/bindings/«template_id»`. The HTTP response codes are:

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
