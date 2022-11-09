(ns com.nytimes.querqy.node.protocols)

(defprotocol Node
  (node-type [this])
  #_(string [this]))

(extend-protocol Node
  Object
  (node-type [_this] :unknown))


(defprotocol InnerNode
  (inner? [node])

  (children [node])

  (replace-children [node children]
    "Returns `node` replacing current children with `children`."))

(extend-protocol InnerNode
  Object
  (inner? [_this] false)
  (children [_this]
    (throw (ex-info "unsupported operation" {})))
  (replace-children [_this _children]
    (throw (ex-info "unsupported operation" {}))))
