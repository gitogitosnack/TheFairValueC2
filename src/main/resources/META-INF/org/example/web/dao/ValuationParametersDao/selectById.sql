select
  /*%expand*/*
from
  valuation_parameters
where
  valuation_model_id = /* valuation_model_id */1
order by
  display_order asc, id asc