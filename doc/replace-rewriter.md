# Replace Rewriter

The Replace Rewriter is mainly intended as a pre-processing step for other
rewriters. Some common use cases are to fix typos, delete unhelpful terms,
and otherwise fix up user input so that subsequent rewriters are simpler to
define.

See the [Querqy docs][1] for more details.

## Usage

### Querqy File

You can read in a text file with replace rules written in the Querqy Replace
Rewriter syntax. See [the docs][1] for the details of that format.

This format is particularly useful if you can source a large list of typos
and their corrections from your query logs.

```clojure
(require '[com.nytimes.querqy.replace :refer [replace-rewriter]])
(require '[clojure.java.io :as io])

(replace-rewriter (io/resource "replace-rules.txt"))
```

### DSL

The `querqy-clj` library ships with a DSL for creating a Replace Rewriter
from Clojure.

```clojure
(require '[com.nytimes.querqy.replace
           :refer [replace-rewriter replace with delete]])

(replace-rewriter
  ;; Fix typos
  (replace "abocado" (with "avocado"))

  ;; Delete useless terms
  (delete "recipe")
  (delete "recipes"))
```

[1]: https://docs.querqy.org/querqy/rewriters/replace.html