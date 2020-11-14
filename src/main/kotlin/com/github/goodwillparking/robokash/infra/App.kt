package com.github.goodwillparking.robokash.infra

import software.amazon.awscdk.core.App


fun main() {


    val app = App()

    RobokashStack(app, "Robokash-dev")
//    RobokashStack(app, "bar")

    app.synth()
}
