# querqy-clj

A library for rewriting search queries to improve relevance.

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

## Docs

- [Intro][2]
- [Replace Rewriter][3]
- [Common Rules Rewriter][4]
- [Emitting Queries][5]
- [Protocols][6]
- [cljfmt][7]

---

[1]: https://docs.querqy.org/
[2]: doc/intro.md
[3]: doc/replace-rewriter.md
[4]: doc/common-rules-rewriter.md
[5]: doc/emitting-queries.md
[6]: doc/protocols.md
[7]: doc/cljfmt.md
