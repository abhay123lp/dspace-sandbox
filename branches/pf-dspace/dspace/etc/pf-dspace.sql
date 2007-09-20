-- pf-dspace.sql
--
-- Version: $Revision: 1.1 $
--
-- Date:	$Date: 2006/10/09 15:36:32 $
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

-- 
-- SQL commands to upgrade the database schema of a live DSpace 1.4 to make use
-- of the patches from the China Digital Museum Project
--
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-- DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST. DUMP YOUR DATABASE FIRST.
-------------------------------------------------------
-- Sequences for creating new IDs (primary keys) for
-- tables.  Each table must have a corresponding
-- sequence called 'tablename_seq'.
-------------------------------------------------------
CREATE SEQUENCE remoterepository_seq;
CREATE SEQUENCE failedimports_seq;

-------------------------------------------------------
-- RemoteRepository table
-------------------------------------------------------
CREATE TABLE remoterepository
(  
	repository_id			INTEGER PRIMARY KEY,
	community_id			INTEGER,
	name					VARCHAR(256) NOT NULL,
	harvest_url				VARCHAR(256) NOT NULL,
	admin_email				VARCHAR(128) NOT NULL,
	is_active				BOOL NOT NULL,
	is_public				BOOL NOT NULL,
	is_alive				BOOL NOT NULL,
	distance				INTEGER,
	date_added				TIMESTAMP WITH TIME ZONE NOT NULL,
	date_last_harvested		TIMESTAMP WITH TIME ZONE NOT NULL,
	date_last_seen			TIMESTAMP WITH TIME ZONE NOT NULL,
	FOREIGN KEY (community_id) REFERENCES community(community_id)
);

-------------------------------------------------------
-- BadRecords table
-------------------------------------------------------
CREATE TABLE failedimports
(
	id					INTEGER PRIMARY KEY,
	repository_id		INTEGER REFERENCES RemoteRepository(repository_id),
	identifier			VARCHAR(256) NOT NULL
);

-- Update the Handle table to hold some extra information
ALTER TABLE Handle ADD COLUMN state INTEGER;
ALTER TABLE Handle ADD COLUMN last_modified TIMESTAMP WITH TIME ZONE;

alter table remoterepository add column uuid varchar(36);
