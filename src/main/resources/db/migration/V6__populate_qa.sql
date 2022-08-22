insert into migrations.test_table(id, env, text_variable)
select '3', 'qa', 'text'
where '${env}' = 'qa';