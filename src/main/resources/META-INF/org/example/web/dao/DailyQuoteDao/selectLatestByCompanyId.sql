select
  /*%expand*/*
from
  daily_quotes
where
  company_id = /* companyId */1
order by
  date desc
limit 1
