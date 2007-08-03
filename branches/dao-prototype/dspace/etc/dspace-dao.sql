alter table handle add column type_id integer;

create view externalidentifier as
(
	select
		handle_id as id,
		handle as value,
		resource_type_id,
		resource_id,
		type_id
	from handle
	order by handle_id
);

alter table bitstream add column uuid varchar(36);
alter table bundle add column uuid varchar(36);
alter table item add column uuid varchar(36);
alter table collection add column uuid varchar(36);
alter table community add column uuid varchar(36);
alter table eperson add column uuid varchar(36);
alter table epersongroup add column uuid varchar(36);
alter table workspaceitem add column uuid varchar(36);
alter table bitstreamformatregistry add column uuid varchar(36);
alter table resourcepolicy add column uuid varchar(36);
alter table workflowitem add column uuid varchar(36);
