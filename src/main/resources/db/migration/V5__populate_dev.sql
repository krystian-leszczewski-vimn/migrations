insert into migrations.test_table(id, env, text_variable)
select '3', 'dev', 'text'
where '${env}' = 'dev';