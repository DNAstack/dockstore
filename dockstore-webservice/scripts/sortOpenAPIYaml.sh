yq r --tojson src/main/resources/openapi3/unsortedopenapi.yaml | jq . | yq r - -P > src/main/resources/openapi3/openapi.yaml
