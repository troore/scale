#include <stdio.h>

#define tn 4
#define wl 128
#define N (32 * tn * wl)

#define SLICE 4
#define slice_num (wl / SLICE)

int E[32 * tn][wl];

int result = 0;

void sum ()
{
	int i, j, k;

	for (i = 0; i < N / wl; i += tn)
	{
		for (k = 0; k < slice_num; k++)
		{
			int result = 0;
			int result1 = 0;
			int result2 = 0;
			int result3 = 0;

			for (j = k * SLICE; j < (k + 1) * SLICE; j++)
			{
				result += E[i][j];
				result1 += E[i + 1][j];
				result2 += E[i + 2][j];
				result3 += E[i + 3][j];
			}

			result += result;
			result += result1;
			result += result2;
			result += result3;
		}
	}
}

int main ()
{
	int i, j;

	for (i = 0; i < N / wl; ++i)
	{
		for (j = 0; j < wl; ++j)
		{
			E[i][j] = 1;
		}
	}

	sum ();

	printf ("result = %d\n", result);

	return 0;
}
