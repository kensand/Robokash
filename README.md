# Robokash

## TODOs
* Script takes raw chat logs + keywords, creates mapping with weights
  * add support for keywords
  * Add support for filtering specific messages
* Support for global keywords that raise odds of all rolls
* Use message sender as keyword
* Allow distinction between exact keyword match and partial match
* Scale probabilities with number of source messages that match keyword
* Put secrets in KMS
* Parse message to determine if it mentions the bot instead of using app_mention event. 
  This is to keep bot from potentially responding twice.

