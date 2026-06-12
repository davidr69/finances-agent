# finances-agent

The finances application has a statement upload feature which
does nothing more than upload the statement and publish a
Kafka message with the contents of the statement.

A separate microservice is required to consume the message and
communicate with the LLM to parse the statement. The LLM does not
know what to do with it, so the microservice will act as a
sub-agent to the LLM.

TODO:
- [ ] Get the question from the database that is specific to the statement that was uploaded.
