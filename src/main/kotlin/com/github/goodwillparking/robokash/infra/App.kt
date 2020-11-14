package com.github.goodwillparking.robokash.infra

import software.amazon.awscdk.core.App


fun main() {


    val app = App()

    RobokashStack(app, "foo")
    RobokashStack(app, "bar")

    app.synth()
}
