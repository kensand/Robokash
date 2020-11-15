# Robokash

## TODOs
* Script takes raw chat logs + keywords, creates mapping with weights
  * Run script at build time
  * Script also needs to remove Slack formatting markers (maybe?)
* Support for global keywords
* Function that responds (potentially) to all messages
* Use message sender as keyword
* Allow distinction between exact keyword match and partial match
* Scale probabilities with number of source messages that match keyword
* delayed responses? Don't want to sleep, maybe slack API supports this.

