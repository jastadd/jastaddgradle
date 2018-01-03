#!/usr/bin/env groovy

node {
	env.JAVA_HOME="${tool 'oracle-jdk-7'}"
	env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"

	stage('Checkout') {
		checkout scm
	}

	stage('Test') {
		sh "./gradlew clean test"
		junit 'build/test-results/**/*.xml'
	}
}
