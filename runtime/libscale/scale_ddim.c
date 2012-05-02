/* PURE _scale_ddim PURE */
double _scale_ddim(double expr1, double expr2)
{
  return ((expr1 > expr2) ? expr1 - expr2 : 0.0);
}
