/* PURE _scale_modll PURE */
long _scale_modll(long expr1, long expr2)
{
  return expr1 - (expr1 / expr2) * expr2;
}
