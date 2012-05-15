#include <stdio.h>

#define N (128 * 128)

int E[N];

int result = 0;

void sum ()
{
	int i;

	for (i = 0; i < N; i++)
	{
		result += E[i];
	}
}

int main ()
{
	int i;

	for (i = 0; i < N; ++i) E[i] = 1;

	sum ();

	printf ("result = %d\n", result);

	return 0;
}
