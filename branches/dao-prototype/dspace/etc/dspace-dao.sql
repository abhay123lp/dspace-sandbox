alter table handle add column type_id integer;

create view persistentidentifier as
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
