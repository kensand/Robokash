#!/usr/bin/env node
import 'source-map-support/register';
import { RobokashStack } from '../lib/robokash-stack';
import { App } from 'aws-cdk-lib';

let stackCreation: { stacks(app: App): RobokashStack[] }
try {
    stackCreation = require("../lib/stack-creation")
} catch (e) {
    console.log(e)
    throw new Error("Must create stack-creation.ts file with stacks() method.")
}

const app = new App()
stackCreation.stacks(app)
