# querqy-clj

Querqy for Clojure.

## Installation

```clojure
[com.nytimes/querqy "0.0.0"]
```

## What is Querqy?

[Querqy][1] is a query rewriting library. It can help you better connect user
intent to the relevance signals in your search index. You can massage user input
into a better query by fixing typos, removing spurious terms, or injecting
filters or synonyms automatically.

## Rationale

- You want to define and maintain rules client side.
- Build a rich vocabulary of rules, beyond what Querqy templates support.
- No ability, or _desire_, to install custom Elasticsearch plugins.

[1]: https://docs.querqy.org/