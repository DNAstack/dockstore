#!/bin/bash

yq r --tojson src/main/resources/openapi3/unsortedopenapi.yaml | yq r - > src/main/resources/openapi3/openapi.yaml
