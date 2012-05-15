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
	int result1, result2, result3, result4;

	for (i = 0; i < N / wl; i += tn)
	{
		for (k = 0; k < slice_num; k++)
		{
			result1 = result2 = result3 = result4 = 0;
		/*	for (j = i * wl + k * SLICE; j < i * wl + (k + 1) * SLICE; j++)
			{
				result1 += E[j];
				result2 += E[j + wl];
				result3 += E[j + 2 * wl];
				result4 += E[j + 3 * wl];
			}*/
			for (j = k * SLICE; j < (k + 1) * SLICE; j++)
			{
				result1 += E[i][j];
				result2 += E[i + 1][j];
				result3 += E[i + 2][j];
				result4 += E[i + 3][j];
			}
			result += (result1 + result2 + result3 + result4);
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
