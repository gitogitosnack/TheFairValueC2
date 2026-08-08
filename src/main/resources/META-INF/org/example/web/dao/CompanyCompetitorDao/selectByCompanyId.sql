select
  /*%expand*/*
from
  company_competitors
where
  company_id = /* companyId */0
  and delete_flg = 0
order by
  id
