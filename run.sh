#!/bin/bash

set -xe

mvn test exec:java -Dexec.mainClass="com.meuprojeto.app.App"
