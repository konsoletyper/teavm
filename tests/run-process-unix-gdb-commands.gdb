if $_isvoid($_exitcode)
  set logging file /dev/stderr
  set logging redirect on
  set logging enabled on
  bt
  set logging enabled off
  quit 128 + $_exitsignal
else
  quit $_exitcode
end
