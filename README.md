# vdc-query-frontend

This is a frontend for the [viper-data-collection backend](https://github.com/viperproject/viper-data-collection).
To query collected data, only this lightweight project has to be included in your code. To address the correct backend instance,
`val API_HOST` in `queryFrontend.Config` has to be set to `http://server_ip/api_port`.

Common datatypes used by both projects are defined in `queryFrontend.DataModels`.

`queryFrontend.APIQueries` contains pre-written queries, which handle data serialization.
