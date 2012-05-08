int a = 0;
int b = 1;

//int a = 10;

//extern int b;

int c = &b;

void main ()
{
	int a = 2;
//	int a = 20;	

	int b = 3;
	
	printf ("%x\n", c);
	{
		int b = 4;
		printf ("%x\n", &b);
		{
			a = 5;
		}
		{
			//int b = 0;
			printf ("%x\n", &b);
			extern int b;
			printf ("%x\n", &b);
			//extern int b;
			//int b = 7;
			//b = a + 8;
			printf ("%d\n", b);
		}
	}
}
