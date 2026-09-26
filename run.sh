#!/bin/bash

set -xe

mvn test exec:java -X -Dexec.mainClass="com.meuprojeto.app.App"
