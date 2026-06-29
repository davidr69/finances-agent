# finances-agent

My custom financial application requires entering transaction manually. With data since the year 2001, that represents a significant amount of data entry time.

I added a statement upload feature which accepts the statement as a PDF and publishes a Kafka message with the contents of the statement.

A separate microservice (this app) was required which serves as a workflow with one agentic decision point. It performs the following steps:
- consumes the message
- parses the PDF
- removes all extraneous information (isolates transactions)
- per transaction, obtain the cosine vector to determine vendor confidence score
- for confidence scores >= 0.80, place directly in staging table
- for confidence scores < 0.80:
  - sends to the LLM to determine vendor using db and general knowledge
  - if a vendor cannot be determined, create a vendor in the database
  - write the transactions to the staging table
- manual review in the web app provides feedback to further refine vectors

## Workflow

![workflow](images/workflow.png)

## Design decisions

The diagram above is a simplified view of the workflow. Three local LLM’s
were tested:
- mistral
- qwen2.5:7b
- gemma4

The commercial LLM that was tested is Anthropic Claude Sonnet 4.6, and upon comparing the results of local LLM’s with Anthropic’s, I settled on Claude as the default for productions and gemma4 for development.

For calculating vectors, there is also a nomic embedding model:
```shell
% curl -s http://llm:11434/api/tags | jq -r '.models[] | "\(.name)\t\(.capabilities | join(","))"'
nomic-embed-text:latest	embedding
gemma4:latest   completion,tools,thinking
qwen2.5:7b      completion,tools
mistral:latest  completion,tools
```

Of the three local LLM‘s, only gemma4 is multi-modal. Despite that, PDF‘s must first be rasterized prior to uploading. Commercial LLM’s do this implicitly without the user’s knowledge, but it must be explicitly done here.

The idea of sending an entire PDF to an LLM and have it figure out everything is misguided and inefficient. Both mistral and qwen2.5:7b were unable to understand how to identify transactions, while gemma4 was able to identify the first two transactions correctly, and then it quickly hallucinated.

Instead, for each statement type, parsing and cleanup must be performed, and then clean data can then be sent as text to the LLM and the response can be requested as structured JSON. This works properly with both gemma4 and Clause, but when I enabled prompt caching in Claude, it decided to also summarize its decisions and appending the JSON response, requiring the response to be parsed.

## Requirements

Several options exist for the vector database, but the simplest was to enable the pgvector functionality in PostGreSQL:

```shell
$ apt install postgresql-16-pgvector
```

Once installed, the extension must be created:

```sql
CREATE EXTENSION vector;
```

Adding a column to my merchant table is very convenient because the data becomes paired. Also note that traditional indexes (e.g. btree) cannot be used since we are measuring vector proximity using cosine, not ordered data:

```sql
ALTER TABLE entities ADD COLUMN embedding vector(768);

CREATE INDEX ON entities USING hnsw (embedding vector_cosine_ops);
```

The reason for making the capacity 768 is due to the response of the embedding model:


```shell
% curl -s http://llm:11434/api/embeddings -d '{"model": "nomic-embed-text", "prompt": "test"}' | jq '.embedding | length'
768
```

### TODO:
- persist proper account
- how do I prevent duplicate records on re-run?
- create deserialization beans for each Kafka consumer