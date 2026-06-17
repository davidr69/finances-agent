# finances-agent

The finances application requires entering every transaction manually. With data since the year 2,001, that represents a significant amount of data entry time.

I added a statement upload feature which does nothing more than accept the statement as a PDF and publish a Kafka message with the contents of the statement.

A separate microservice is required, which serves as a workflow with one agentic decision point. It performs the following steps:
- consumes the message
- parses the PDF
- removes all extraneous information
- consults the vector database to obtain confidence scores for vendor names for each transaction
- creates a sublist for confidence matches below 80%
- sends the sublist to the LLM for it to determine the vendor
  - if a vendor cannot be determined, create a vendor in the database

Transactions can then be written to a staging table for manual review.

Context is preserved in the database so that a restart or switch of LLM preserves "memory".

## Conceptual idea

![workflow](images/workflow.png)

## Design decisions

The diagram above is a simplified view of the workflow. Three LLM's
were tested:
- mistral
- qwen2.5:7b
- gemma4

For calculating vectors, there is also a nomic embedding model:
```shell
% curl -s http://llm:11434/api/tags | jq -r '.models[] | "\(.name)\t\(.capabilities | join(","))"'
nomic-embed-text:latest	embedding
gemma4:latest   completion,tools,thinking
qwen2.5:7b      completion,tools
mistral:latest  completion,tools
```

Of the three LLM‘s, only gemma4 is multi-modal. This means it can accept more than just text as input. Despite gemma4 being multi-modal, PDF‘s must first be rasterized prior to uploading. Commercial LLM's do this implicitly without the user being aware, but it must be explicitly done here.

The initial idea was to send PDF‘s to the LLM and have it figure out everything, returning JSON-formatted transactions. Both mistral and qwen2.5:7b were unable to understand how to identify transactions. gemma4 was able to identify the first two transactions correctly, and then it quickly hallucinated.

Implementing pre-LLM logic became unavoidable. For each statement type,parsing and cleanup must be performed, and then clean data can then be sent as text to the LLM. At a minimum, this adds some level of determinism to the process rather than expecting the LLM figure out statement formats and map vendors with no frame of reference.

## Requirements

Several options exist for the vector database, but the simplest was to enable the pgvector functionality in PostGreSQL:

```shell
$ apt install postgresql-16-pgvector
```
```sql
CREATE EXTENSION vector;

ALTER TABLE entities ADD COLUMN embedding vector(768);
```

The reason for making the capacity 768 is due to the response of the embedding model:

```shell
% curl -s http://llm:11434/api/embeddings -d '{"model": "nomic-embed-text", "prompt": "test"}' | jq '.embedding | length'
768
```

### misc
Semantic search system

Semantic understanding    ← nomic-embed-text (legit AI)  
Similarity reasoning      ← pgvector cosine distance  
Decision threshold        ← 0.80 rule


✅ Kafka consumer (entry point)  
✅ PDFBox text extraction  
✅ StatementParser (Strategy pattern)  
✅ pgvector + nomic-embed-text (vendor similarity)  
✅ Tool architecture clarified (@Tool, local, no listener needed)  
✅ Wire VendorTools into ChatClient  
✅ Agentic loop (findVendor → createVendor if needed)  
⬜ INSERT transactions into financial data  
⬜ Manual review stage