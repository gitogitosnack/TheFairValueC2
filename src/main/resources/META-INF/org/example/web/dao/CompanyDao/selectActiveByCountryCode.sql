select
  /*%expand*/*
from
  companies
where
  delete_flg = 0
and
  country_id = (select id from countries where code = /* countryCode */'US')
order by
  id
