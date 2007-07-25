--
-- update-sequences.sql
--
-- Version: $Revision$
--
-- Date:    $Date$
--
-- Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
-- Institute of Technology.  All rights reserved.
-- 
-- Redistribution and use in source and binary forms, with or without
-- modification, are permitted provided that the following conditions are
-- met:
-- 
-- - Redistributions of source code must retain the above copyright
-- notice, this list of conditions and the following disclaimer.
-- 
-- - Redistributions in binary form must reproduce the above copyright
-- notice, this list of conditions and the following disclaimer in the
-- documentation and/or other materials provided with the distribution.
-- 
-- - Neither the name of the Hewlett-Packard Company nor the name of the
-- Massachusetts Institute of Technology nor the names of their
-- contributors may be used to endorse or promote products derived from
-- this software without specific prior written permission.
-- 
-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
-- ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
-- LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
-- A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
-- HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
-- INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
-- BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
-- OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
-- ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
-- TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
-- USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
-- DAMAGE.

-- SQL code to update the ID (primary key) generating sequences, if some
-- import operation has set explicit IDs.
--
-- Sequences are used to generate IDs for new rows in the database.  If a
-- bulk import operation, such as an SQL dump, specifies primary keys for
-- imported data explicitly, the sequences are out of sync and need updating.
-- This SQL code does just that.
--
-- This should rarely be needed; any bulk import should be performed using the
-- org.dspace.content API which is safe to use concurrently and in multiple
-- JVMs.  The SQL code below will typically only be required after a direct
-- SQL data dump from a backup or somesuch.
 

-- There should be one of these calls for every ID sequence defined in
-- database_schema.sql.


SELECT setval('bitstreamformatregistry_seq', max(bitstream_format_id)) FROM bitstreamformatregistry;
SELECT setval('fileextension_seq', max(file_extension_id)) FROM fileextension;
SELECT setval('bitstream_seq', max(bitstream_id)) FROM bitstream;
SELECT setval('eperson_seq', max(eperson_id)) FROM eperson;
SELECT setval('epersongroup_seq', max(eperson_group_id)) FROM epersongroup;
SELECT setval('group2group_seq', max(id)) FROM group2group;
SELECT setval('group2groupcache_seq', max(id)) FROM group2groupcache;
SELECT setval('item_seq', max(item_id)) FROM item;
SELECT setval('bundle_seq', max(bundle_id)) FROM bundle;
SELECT setval('item2bundle_seq', max(id)) FROM item2bundle;
SELECT setval('bundle2bitstream_seq', max(id)) FROM bundle2bitstream;
SELECT setval('dctyperegistry_seq', max(dc_type_id)) FROM dctyperegistry;
SELECT setval('dcvalue_seq', max(dc_value_id)) FROM dcvalue;
SELECT setval('community_seq', max(community_id)) FROM community;
SELECT setval('community2community_seq', max(id)) FROM community2community;
SELECT setval('collection_seq', max(collection_id)) FROM collection;
SELECT setval('community2collection_seq', max(id)) FROM community2collection;
SELECT setval('collection2item_seq', max(id)) FROM collection2item;
SELECT setval('resourcepolicy_seq', max(policy_id)) FROM resourcepolicy;
SELECT setval('epersongroup2eperson_seq', max(id)) FROM epersongroup2eperson;
SELECT setval('handle_seq', max(handle_id)) FROM handle;
SELECT setval('workspaceitem_seq', max(workspace_item_id)) FROM workspaceitem;
SELECT setval('workflowitem_seq', max(workflow_id)) FROM workflowitem;
SELECT setval('tasklistitem_seq', max(tasklist_id)) FROM tasklistitem;
SELECT setval('registrationdata_seq', max(registrationdata_id)) FROM registrationdata;
SELECT setval('subscription_seq', max(subscription_id)) FROM subscription;
SELECT setval('history_seq', max(history_id)) FROM history;
SELECT setval('historystate_seq', max(history_state_id)) FROM historystate;
SELECT setval('communities2item_seq', max(id)) FROM communities2item;
SELECT setval('itemsbyauthor_seq', max(items_by_author_id)) FROM itemsbyauthor;
SELECT setval('itemsbytitle_seq', max(items_by_title_id)) FROM itemsbytitle;
SELECT setval('itemsbydate_seq', max(items_by_date_id)) FROM itemsbydate;
SELECT setval('itemsbydateaccessioned_seq', max(items_by_date_accessioned_id)) FROM itemsbydateaccessioned;
SELECT setval('epersongroup2workspaceitem_seq', max(id)) FROM epersongroup2workspaceitem;
SELECT setval('metadatafieldregistry_seq', max(metadata_field_id)) FROM metadatafieldregistry;
SELECT setval('metadatavalue_seq', max(metadata_value_id)) FROM metadatavalue;
SELECT setval('metadataschemaregistry_seq', max(metadata_schema_id)) FROM metadataschemaregistry;