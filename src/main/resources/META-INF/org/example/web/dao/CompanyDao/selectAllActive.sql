select
  /*%expand*/*
from
  companies
where
  delete_flg = 0
order by
  id
