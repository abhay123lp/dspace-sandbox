-- Database schema file for the Quambo recommender system extension

-- Database table for Research Contexts holding the name of the Research
-- Context, the ID of the EPerson who created it, the minimum relevance of an
-- item to a Research Context for a recommendation to be generated, and a UUID

CREATE TABLE quambo_research_context
(
  research_context_id         INTEGER PRIMARY KEY,
  name                        VARCHAR,
  eperson_id                  INTEGER REFERENCES eperson(eperson_id),
  min_similarity_threshold    INTEGER,
  local_essence_id            INTEGER, --REFERENCES quambo_essence(essence_id)
  uuid                        VARCHAR(36) UNIQUE
);

CREATE SEQUENCE quambo_research_context_seq;

-- Database table for Bookmarks holding the bookmarked Item ID,
-- the Research Context ID, and when the bookmark was created.

CREATE TABLE quambo_bookmark
(
  bookmark_id         INTEGER PRIMARY KEY,
  item_id             INTEGER REFERENCES item(item_id),
  research_context_id INTEGER REFERENCES quambo_research_context(research_context_id),
  created             VARCHAR
);

CREATE SEQUENCE quambo_bookmark_seq;

-- Database table for EPerson specific properies: their initial Research
-- Context ID to prevent deleting it, and the ID of the last Research Context
-- viewed so when they return to their Research Contexts they are back where
-- they were.

CREATE TABLE quambo_eperson
(
  property_id                   INTEGER PRIMARY KEY,
  eperson_id                    INTEGER REFERENCES eperson(eperson_id),
  initial_research_context_uuid VARCHAR(36) REFERENCES quambo_research_context(uuid),
  last_research_context_uuid    VARCHAR(36) REFERENCES quambo_research_context(uuid)
);

CREATE SEQUENCE quambo_eperson_seq;

-- Database table for Item Recommendations holding the ID of the Item, the ID
-- of the Research Context, the relevance of this Item to this Research Context
-- and a date of creation of the recommendation.

CREATE TABLE quambo_item2research_context_recommendation
(
  i2rcr_id             INTEGER PRIMARY KEY,
  item_id              INTEGER REFERENCES item(item_id),
  research_context_id  INTEGER REFERENCES quambo_research_context(research_context_id),
  relevance            INTEGER,
  uuid                 VARCHAR(36) UNIQUE
);

CREATE SEQUENCE quambo_item2research_context_recommendation_seq;

CREATE TABLE quambo_item2research_context_recommendation_status
(
  status_id     INTEGER PRIMARY KEY,
  i2rcr_id      INTEGER REFERENCES quambo_item2research_context_recommendation(i2rcr_id),
  last_updated  VARCHAR(28)
);

CREATE SEQUENCE quambo_item2research_context_recommendation_status_seq;

-- Database table for Item-to-Item recommendations shown alongside items, holds
-- the ID of the Item being shown and the ID of the Item recommended, the
-- similarity of the Item being shown to the Item being recommended, and
-- the date this Item-to-Item recommendation was created.

CREATE TABLE quambo_item2item_recommendation
(
    i2ir_id     INTEGER PRIMARY KEY,
    item1_id    INTEGER REFERENCES item(item_id),
    item2_id    INTEGER REFERENCES item(item_id),
    similarity  INTEGER,
    uuid        VARCHAR(36) UNIQUE
);

CREATE SEQUENCE quambo_item2item_recommendation_seq;

CREATE TABLE quambo_item2item_recommendation_status
(
  status_id     INTEGER PRIMARY KEY,
  item_id       INTEGER REFERENCES item(item_id),
  last_updated  VARCHAR(28)
);

CREATE SEQUENCE quambo_item2item_recommendation_status_seq;                       

CREATE TABLE quambo_essence
(
  essence_id            INTEGER PRIMARY KEY,
  uri                   VARCHAR,
  weight                INTEGER,
  research_context_id   INTEGER REFERENCES quambo_research_context(research_context_id),
  name                  VARCHAR
);

CREATE SEQUENCE quambo_essence_seq;

-- Database table for the (key, value) pairs representing the characterisation
-- of a Research Context as a result of the metadata extracted from Bookmarks,
-- or remote Essence feeds. Type is one of: keyword, tag, tfidf, author

CREATE TABLE quambo_key_value
(
  key_value_id          INTEGER PRIMARY KEY,
  key                   VARCHAR,
  value                 INTEGER,
  essence_id            INTEGER REFERENCES quambo_essence(essence_id),
  research_context_id   INTEGER REFERENCES quambo_research_context(research_context_id),
  type                  VARCHAR
);

CREATE SEQUENCE quambo_key_value_seq;