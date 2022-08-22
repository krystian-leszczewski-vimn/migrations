insert into migrations.test_table(id, env, text_variable)
select '3', 'prod', 'text'
where '${env}' = 'prod';