select
  /*%expand*/*
from
  financial_statements
where
  company_id = /* companyId */1
order by
  fiscal_year desc,
  fiscal_quarter desc
limit 1
