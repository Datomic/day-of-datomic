;; given some initial schema
{:db/ident :user/id
 :db/valueType :db.type/string
 :db/cardinality :db.cardinality/one}

;; 4. Growing is adding.
{:db/ident :user/name
 :db/valueType :db.type/string
 :db/cardinality :db.cardinality/one}

;; 5. Never remove a name.
;; 6. Never reuse a name.
;; (this space intentionally blank)

;; instead of 5/6, you can 7. Use aliases
[:db/add :user/id :db/ident :user/primary-email]

;; 8. Namespace all names.
[:db/add :user/name :db/ident :user2/name]
{:db/ident :user2/id
 :db/valueType :db.type/uuid
 :db/cardinality :db.cardinality/one}

;; Instead of destructive change, you can
;; 9. Annotate your schema
{:db/ident :user
 :schema/see-instead :user2
 :db/doc "prefer the user2 namespace for new development"}

;; 10. plan for accretion
(do-side-effecty-thing-with-each
 (select-keys some-entity keys-i-understand))
;; not (do-side-effecty-thing-with-each some-entity)



