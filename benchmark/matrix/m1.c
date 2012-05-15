#include <stdlib.h>
#include <time.h>

#ifdef MATRIX_32
#define N 32
#else
#define N 64
#endif

#define SLICE 4
#define slice_num (N / SLICE)

float a[N][N],b[N][N],c[N][N];

void matrix_multiply()
{
	int i, j, k, h;

	for(i = 0;i < N;i++)
	{
		for(j = 0; j < N; j += 4)
		{
			c[i][j] = 0;
			c[i][j + 1] = 0;
			c[i][j + 2] = 0;
			c[i][j + 3] = 0;

			for (h = 0; h < slice_num; h++)
			{
				for(k = h * SLICE; k < (h + 1) * SLICE; k++)
				{
					c[i][j] += a[i][k] * b[k][j];
					c[i][j + 1] += a[i][k] * b[k][j + 1];
					c[i][j + 2] += a[i][k] * b[k][j + 2];
					c[i][j + 3] += a[i][k] * b[k][j + 3];
				}
			}
		}
	}
}

int main()
{
	int i,j;
	srand(time(NULL));
	for(i = 0;i < N;i++)
		for(j = 0;j < N;j++)
		{
			a[i][j] = (float)rand() / rand();
			b[i][j] = (float)rand() / rand();
		}
	matrix_multiply();
	return 0;
}
