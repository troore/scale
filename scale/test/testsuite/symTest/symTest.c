int a = 0;
int b = 1;

int a = 10;

extern int c;

int d = &b;

void main ()
{
	int a = 2;
	int a = 20;	

	int b = 3;
	
	printf ("%x\n", d);
	{
		int b = 4;
		printf ("%x\n", &b);
		{
			a = 5;
		}
		{
			extern int b;
			int h = b;
			//int b = 7;
			b = 7;
			printf ("%x\n", &b);
			printf ("%d\n", b);
			printf ("%d\n", h);
		}
	}
}
