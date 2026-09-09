select
  /*%expand*/*
from
  daily_quotes
where
  company_id = /* companyId */1
and
  date = /* date */'2025-01-01'
