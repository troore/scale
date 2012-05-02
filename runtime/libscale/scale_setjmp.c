extern void exit(int);
int _scale_setjmp(void *add)
{
  exit(1);
  return 0;
}

void _scale_longjmp(void *add, int x)
{
  exit(1);
}
