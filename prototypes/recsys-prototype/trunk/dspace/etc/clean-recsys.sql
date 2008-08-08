-- Clears the database tables and sequences added by the Quambo recommender
-- system add-on

DROP TABLE quambo_key_value;
DROP TABLE quambo_essence;
DROP TABLE quambo_item2item_recommendation_status;
DROP TABLE quambo_item2researchcontext_recommendation_status;
DROP TABLE quambo_item2item_recommendation;
DROP TABLE quambo_item2researchcontext_recommendation;
DROP TABLE quambo_eperson;
DROP TABLE quambo_bookmark;
DROP TABLE quambo_research_context;

DROP SEQUENCE quambo_research_context_seq;
DROP SEQUENCE quambo_eperson_seq;
DROP SEQUENCE quambo_bookmark_seq;
DROP SEQUENCE quambo_item2item_recommendation_seq;
DROP SEQUENCE quambo_item2researchcontext_recommendation_seq;
DROP SEQUENCE quambo_key_value_seq;
DROP SEQUENCE quambo_essence_seq;
