select
  /*%expand*/*
from
  financial_statements
where
  company_id = /* companyId */1
and
  fiscal_quarter = 'Q4'
order by
  fiscal_year desc
limit 1
