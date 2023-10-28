# Bank0 Demo Android App

Its purpose is to complement Bank0 Demo app (see https://github.com/auth0/auth0-demo-bank0).

It's based on https://github.com/auth0/Guardian.Android but extracted here with the intention to
extend it in the future.

At the moment the difference Guardian Android app is that accepts custom data from from MFA push and
displays it. Beside this it allows to enroll in MFA and receive MFA push requests.

### Guardian SDK

This repo also contains local version of Guardian SDK, that can be enhanced and tested together with
Bank0. However in the end, we will need to create the corresponding PR to the main
repo https://github.com/auth0/Guardian.Android.

## Configuration

As a pre-requisite it's recommended to go through the docs for original Guardian sample app:

- https://github.com/auth0/Guardian.Android
- https://oktawiki.atlassian.net/wiki/spaces/MFA/pages/2541750656/Android+Demo+App+with+Vivaldi
