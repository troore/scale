;# Set scale.score.Scribble.doIfConversion = 0.
;# Set scale.backend.trips2.Trips2Machine.softwareFDIV = 1.
;# Set scale.backend.trips2.HyperblockFormation.unrollForLoops = 0.
;# Set scale.backend.trips2.HyperblockFormation.unrollLoops = 1.
;# Set scale.backend.trips2.HyperblockFormation.tailDuplicate = 0.
;# Set scale.backend.trips2.HyperblockFormation.peelLoops = 1.
;# Set scale.score.trans.URJ.inhibitWhileLoops = 1.
;# Set scale.backend.trips2.Trips2Machine.tcconfigPath = "/home/troore/Projects/trips/ttools2/etc/config/default".
;# Target architecture: trips.
;# Host architecture: i386.
;# Simple intra-procedural alias analysis.
;# Separate compilation.
;# Scale Compiler Version:  Fri Jul 20 00:07:46 CDT 2007 
;# Inlining level: 1.1.
;# Data dependence testing: tibBO.
;# Optimizations: a1c0fgjcamnpxnmpibudl.
;# Performing: Structure Fields in Registers
;# Performing: Global Variable Replacement
;# Performing: Flatten, Unroll & Jam
;# Performing: Sparse Conditional Constant Propagation
;# Performing: Array Access Strength Reduction
;# Performing: Loop Invariant Code Motion
;# Performing: Global Value Numbering
;# Performing: Copy Propagation
;# Performing: Scalar Replacement
;# Performing: Global Value Numbering
;# Performing: Loop Invariant Code Motion
;# Performing: Copy Propagation
;# Performing: Expression Tree Height Reduction
;# Performing: Basic Block Optimizations
;# Performing: Useless Copy Removal
;# Performing: Dead Variable Elimination
;# Performing: Loop Test at End
;# Backend hyperblock formation enabled.
;# Scale -oa -quiet -arch trips2 -f scale.score.Scribble.doIfConversion=0 -I/home/troore/Projects/trips/ttools2/usr/include -Ofgjcamnpxnmpibudl -hb backend -f scale.backend.trips2.Trips2Machine.softwareFDIV=1 -f scale.backend.trips2.HyperblockFormation.unrollForLoops=0 -f scale.backend.trips2.HyperblockFormation.unrollLoops=1 -f scale.backend.trips2.HyperblockFormation.tailDuplicate=0 -f scale.backend.trips2.HyperblockFormation.peelLoops=1 -f scale.score.trans.URJ.inhibitWhileLoops=1 -inl 10 -AA1 -f scale.backend.trips2.Trips2Machine.tcconfigPath=/home/troore/Projects/trips/ttools2/etc/config/default reduce.c -dir ..
.app-file "reduce.c"
; BSS 
	.global B
	.comm	B, 160, 4
	.global C
	.comm	C, 160, 4

	.data	
	.align	8
_V0:
	.ascii	"Sum of two arrays B and C is: %d\n\000"
	.align	4
	.global	thread_num
thread_num:
	.int	0x4

	.text	
	.global	reduce
;VARIABLE "local_len" size:4 $g76
;VARIABLE "i" size:4 $g75
;VARIABLE "j" size:4 $g71
;VARIABLE "sum" size:4 $g80
;VARIABLE "local_sum" size:4 $g70
;VARIABLE "_gr0" size:4 $g81
;VARIABLE "_ar0" size:8 $g73
;VARIABLE "_ar1" size:8 $g72
;VARIABLE "_ar2" size:8 $g74
;VARIABLE "_ar3" size:8 $g77
;VARIABLE "_li0" size:4 $g79
;VARIABLE "_li1" size:4 $g74
;VARIABLE "j#s_29_t_0" size:4 $g78
;VARIABLE "j_ssa_0" size:4 $g73
;VARIABLE "local_sum_ssa_1" size:4 $g72
.bbegin reduce
	read	$t0, $g1
	addi	$t1, $t0, -32
	sd	($t1), $t0 S[0]
	entera	$t2, thread_num
	lws	$t3, ($t2) L[1]
	movi	$t4, 40
	divs	$t5, $t4, $t3
	tgti	$p6, $t3, 0
	movi_t<$p6>	$t7, 0
	movi_t<$p6>	$t8, 0
	bro_t<$p6>	reduce$1		; loop 1
	bro_f<$p6>	reduce$9
	null_f<$p6>	$t7
	null_f<$p6>	$t8
	write	$g1, $t1
	write	$g75, $t8
	write	$g76, $t5
	write	$g80, $t7
	write	$g81, $t3
.bend
.bbegin reduce$1
	read	$t0, $g75
	read	$t1, $g76
	mul	$t2, $t0, $t1		; loop 1
	extsw	$t3, $t2		; loop 1
	addi	$t4, $t0, 1		; loop 1
	extsw	$t5, $t4		; loop 1
	mul	$t6, $t5, $t1		; loop 1
	extsw	$t7, $t6		; loop 1
	tlt	$p8, $t3, $t7		; loop 1
	subi	$t9, $t7, 2		; loop 1
	extsw	$t10, $t9		; loop 1
	tge_t<$p8>	$p11, $t3, $t10		; loop 1
	bro_f<$p8>	reduce$8		; loop 1
	null_f<$p8>	$t12
	null_f<$p8>	$t13
	null_f<$p8>	$t14
	null_f<$p8>	$t15
	null_f<$p8>	$t16
	null_f<$p8>	$t17
	null_f<$p8>	$t18
	movi_t<$p11>	$t18, 0
	bro_t<$p11>	reduce$6
	null_t<$p11>	$t12
	null_t<$p11>	$t13
	null_t<$p11>	$t14
	null_t<$p11>	$t15
	null_t<$p11>	$t16
	null_t<$p11>	$t17
	slli	$t19, $t3, 2		; loop 1
	entera	$t20, B		; loop 1
	add	$t21, $t20, $t19		; loop 1
	entera	$t22, C		; loop 1
	add	$t23, $t22, $t19		; loop 1
	movi	$t24, 0
	lws	$t25, ($t21) L[0]		; loop 4
	lws	$t26, ($t23) L[1]		; loop 4
	lws	$t27, 4($t21) L[2]		; loop 4
	add	$t28, $t26, $t27		; loop 4
	extsw	$t29, $t28		; loop 4
	lws	$t30, 4($t23) L[3]		; loop 4
	add	$t31, $t30, $t24		; loop 4
	extsw	$t32, $t31		; loop 4
	lws	$t33, 8($t23) L[4]		; loop 4
	add	$t34, $t33, $t29		; loop 4
	extsw	$t35, $t34		; loop 4
	lws	$t36, 8($t21) L[5]		; loop 4
	add	$t37, $t25, $t36		; loop 4
	extsw	$t38, $t37		; loop 4
	add	$t39, $t32, $t38		; loop 4
	extsw	$t40, $t39		; loop 4
	add	$t41, $t35, $t40		; loop 4
	extsw	$t42, $t41		; loop 4
	addi	$t43, $t3, 3		; loop 4
	extsw	$t44, $t43		; loop 4
	addi	$t45, $t21, 12		; loop 4
	addi	$t46, $t23, 12		; loop 4
	tlt_f<$p11>	$p47, $t44, $t10		; loop 4
	mov_t<$p47>	$t12, $t10
	mov_t<$p47>	$t13, $t3
	mov_t<$p47>	$t14, $t46
	mov_t<$p47>	$t15, $t45
	mov_t<$p47>	$t16, $t44
	mov_t<$p47>	$t17, $t42
	mov_t<$p47>	$t18, $t42
	bro_t<$p47>	reduce$5		; loop 4
	mov_f<$p47>	$t12, $t10
	mov_f<$p47>	$t13, $t3
	mov_f<$p47>	$t14, $t46
	mov_f<$p47>	$t15, $t45
	mov_f<$p47>	$t16, $t44
	mov_f<$p47>	$t17, $t42
	mov_f<$p47>	$t18, $t42
	bro_f<$p47>	reduce$2		; loop 4
	write	$g70, $t18
	write	$g71, $t3
	write	$g72, $t17
	write	$g73, $t16
	write	$g74, $t15
	write	$g77, $t14
	write	$g78, $t13
	write	$g79, $t12
.bend
.bbegin reduce$2
	read	$t0, $g75
	read	$t1, $g76
	read	$t2, $g78
	addi	$t3, $t2, 3		; loop 1
	extsw	$t4, $t3		; loop 1
	addi	$t5, $t0, 1		; loop 1
	extsw	$t6, $t5		; loop 1
	mul	$t7, $t6, $t1		; loop 1
	extsw	$t8, $t7		; loop 1
	tlt	$p9, $t4, $t8		; loop 1
	extsw_t<$p9>	$t10, $t3		; loop 1
	bro_t<$p9>	reduce$6		; loop 1
	bro_f<$p9>	reduce$3		; loop 1
	null_f<$p9>	$t10
	write	$g71, $t10
.bend
.bbegin reduce$3
	read	$t0, $g70
	read	$t1, $g75
	read	$t2, $g80
	read	$t3, $g81
	add	$t4, $t2, $t0		; loop 1
	extsw	$t5, $t4		; loop 1
	addi	$t6, $t1, 1		; loop 1
	extsw	$t7, $t6		; loop 1
	tlt	$p8, $t7, $t3		; loop 1
	bro_t<$p8>	reduce$1		; loop 1
	bro_f<$p8>	reduce$4		; loop 1
	write	$g75, $t7
	write	$g80, $t5
.bend
.bbegin reduce$4
	read	$t0, $g1
	read	$t1, $g2
	read	$t2, $g80
	extsw	$t3, $t2
	addi	$t4, $t0, 32
	ret	$t1
	write	$g1, $t4
	write	$g3, $t3
.bend
.bbegin reduce$5
	read	$t0, $g72
	read	$t1, $g73
	read	$t2, $g74
	read	$t3, $g77
	read	$t4, $g79
	lws	$t5, ($t2) L[0]		; loop 4
	lws	$t6, ($t3) L[1]		; loop 4
	lws	$t7, 4($t2) L[2]		; loop 4
	add	$t8, $t6, $t7		; loop 4
	extsw	$t9, $t8		; loop 4
	lws	$t10, 4($t3) L[3]		; loop 4
	add	$t11, $t10, $t0		; loop 4
	extsw	$t12, $t11		; loop 4
	lws	$t13, 8($t3) L[4]		; loop 4
	add	$t14, $t13, $t9		; loop 4
	extsw	$t15, $t14		; loop 4
	lws	$t16, 8($t2) L[5]		; loop 4
	add	$t17, $t5, $t16		; loop 4
	extsw	$t18, $t17		; loop 4
	add	$t19, $t12, $t18		; loop 4
	extsw	$t20, $t19		; loop 4
	add	$t21, $t15, $t20		; loop 4
	extsw	$t22, $t21		; loop 4
	addi	$t23, $t1, 3		; loop 4
	extsw	$t24, $t23		; loop 4
	addi	$t25, $t2, 12		; loop 4
	addi	$t26, $t3, 12		; loop 4
	addi	$t27, $t1, 3		; loop 4
	extsw	$t28, $t27		; loop 4
	tlt	$p29, $t28, $t4		; loop 4
	lws	$t30, ($t25) L[6]		; loop 4
	lws	$t31, ($t26) L[7]		; loop 4
	lws	$t32, 4($t25) L[8]		; loop 4
	add	$t33, $t31, $t32		; loop 4
	extsw	$t34, $t33		; loop 4
	lws	$t35, 4($t26) L[9]		; loop 4
	add	$t36, $t35, $t22		; loop 4
	extsw	$t37, $t36		; loop 4
	lws	$t38, 8($t26) L[10]		; loop 4
	add	$t39, $t38, $t34		; loop 4
	extsw	$t40, $t39		; loop 4
	lws	$t41, 8($t25) L[11]		; loop 4
	add	$t42, $t30, $t41		; loop 4
	extsw	$t43, $t42		; loop 4
	add	$t44, $t37, $t43		; loop 4
	extsw	$t45, $t44		; loop 4
	add	$t46, $t40, $t45		; loop 4
	extsw	$t47, $t46		; loop 4
	addi	$t48, $t24, 3		; loop 4
	extsw	$t49, $t48		; loop 4
	addi	$t50, $t25, 12		; loop 4
	addi	$t51, $t26, 12		; loop 4
	tlt_t<$p29>	$p52, $t49, $t4		; loop 4
	mov_f<$p29>	$t53, $t1
	mov_f<$p29>	$t54, $t26
	mov_f<$p29>	$t55, $t25
	mov_f<$p29>	$t56, $t24
	mov_f<$p29>	$t57, $t22
	mov_f<$p29>	$t58, $t22
	bro_f<$p29>	reduce$2		; loop 4
	mov_t<$p52>	$t53, $t24
	mov_t<$p52>	$t54, $t51
	mov_t<$p52>	$t55, $t50
	mov_t<$p52>	$t56, $t49
	mov_t<$p52>	$t57, $t47
	mov_t<$p52>	$t58, $t47
	bro_t<$p52>	reduce$5		; loop 4
	mov_f<$p52>	$t53, $t24
	mov_f<$p52>	$t54, $t51
	mov_f<$p52>	$t55, $t50
	mov_f<$p52>	$t56, $t49
	mov_f<$p52>	$t57, $t47
	mov_f<$p52>	$t58, $t47
	bro_f<$p52>	reduce$2		; loop 4
	write	$g70, $t58
	write	$g72, $t57
	write	$g73, $t56
	write	$g74, $t55
	write	$g77, $t54
	write	$g78, $t53
.bend
.bbegin reduce$6
	read	$t0, $g70
	read	$t1, $g71
	read	$t2, $g75
	read	$t3, $g76
	slli	$t4, $t1, 2		; loop 1
	entera	$t5, B		; loop 1
	add	$t6, $t5, $t4		; loop 1
	entera	$t7, C		; loop 1
	add	$t8, $t7, $t4		; loop 1
	addi	$t9, $t2, 1		; loop 1
	extsw	$t10, $t9		; loop 1
	mul	$t11, $t10, $t3		; loop 1
	extsw	$t12, $t11		; loop 1
	lws	$t13, ($t6) L[0]		; loop 2
	lws	$t14, ($t8) L[1]		; loop 2
	add	$t15, $t13, $t14		; loop 2
	extsw	$t16, $t15		; loop 2
	add	$t17, $t0, $t16		; loop 2
	extsw	$t18, $t17		; loop 2
	addi	$t19, $t1, 1		; loop 2
	extsw	$t20, $t19		; loop 2
	addi	$t21, $t6, 4		; loop 2
	addi	$t22, $t8, 4		; loop 2
	tlt	$p23, $t20, $t12		; loop 2
	lws	$t24, ($t21) L[2]		; loop 2
	lws	$t25, ($t22) L[3]		; loop 2
	add	$t26, $t24, $t25		; loop 2
	extsw	$t27, $t26		; loop 2
	add	$t28, $t18, $t27		; loop 2
	extsw	$t29, $t28		; loop 2
	addi	$t30, $t20, 1		; loop 2
	extsw	$t31, $t30		; loop 2
	addi	$t32, $t21, 4		; loop 2
	addi	$t33, $t22, 4		; loop 2
	tlt_t<$p23>	$p34, $t31, $t12		; loop 2
	mov_f<$p23>	$t35, $t21
	mov_f<$p23>	$t36, $t22
	mov_f<$p23>	$t37, $t20
	mov_f<$p23>	$t38, $t18
	bro_f<$p23>	reduce$3		; loop 2
	lws	$t39, ($t32) L[4]		; loop 2
	lws	$t40, ($t33) L[5]		; loop 2
	add	$t41, $t39, $t40		; loop 2
	extsw	$t42, $t41		; loop 2
	add	$t43, $t29, $t42		; loop 2
	extsw	$t44, $t43		; loop 2
	addi	$t45, $t31, 1		; loop 2
	extsw	$t46, $t45		; loop 2
	addi	$t47, $t32, 4		; loop 2
	addi	$t48, $t33, 4		; loop 2
	tlt_t<$p34>	$p49, $t46, $t12		; loop 2
	mov_f<$p34>	$t35, $t32
	mov_f<$p34>	$t36, $t33
	mov_f<$p34>	$t37, $t31
	mov_f<$p34>	$t38, $t29
	bro_f<$p34>	reduce$3		; loop 2
	lws	$t50, ($t47) L[6]		; loop 2
	lws	$t51, ($t48) L[7]		; loop 2
	add	$t52, $t50, $t51		; loop 2
	extsw	$t53, $t52		; loop 2
	add	$t54, $t44, $t53		; loop 2
	extsw	$t55, $t54		; loop 2
	addi	$t56, $t46, 1		; loop 2
	extsw	$t57, $t56		; loop 2
	addi	$t58, $t47, 4		; loop 2
	addi	$t59, $t48, 4		; loop 2
	tlt_t<$p49>	$p60, $t57, $t12		; loop 2
	mov_f<$p49>	$t35, $t47
	mov_f<$p49>	$t36, $t48
	mov_f<$p49>	$t37, $t46
	mov_f<$p49>	$t38, $t44
	bro_f<$p49>	reduce$3		; loop 2
	mov_t<$p60>	$t35, $t58
	mov_t<$p60>	$t36, $t59
	mov_t<$p60>	$t37, $t57
	mov_t<$p60>	$t38, $t55
	bro_t<$p60>	reduce$7		; loop 2
	mov_f<$p60>	$t35, $t58
	mov_f<$p60>	$t36, $t59
	mov_f<$p60>	$t37, $t57
	mov_f<$p60>	$t38, $t55
	bro_f<$p60>	reduce$3		; loop 2
	write	$g70, $t38
	write	$g71, $t37
	write	$g72, $t36
	write	$g73, $t35
	write	$g74, $t12
.bend
.bbegin reduce$7
	read	$t0, $g70
	read	$t1, $g71
	read	$t2, $g72
	read	$t3, $g73
	read	$t4, $g74
	lws	$t5, ($t3) L[0]		; loop 2
	lws	$t6, ($t2) L[1]		; loop 2
	add	$t7, $t5, $t6		; loop 2
	extsw	$t8, $t7		; loop 2
	add	$t9, $t0, $t8		; loop 2
	extsw	$t10, $t9		; loop 2
	addi	$t11, $t1, 1		; loop 2
	extsw	$t12, $t11		; loop 2
	addi	$t13, $t3, 4		; loop 2
	addi	$t14, $t2, 4		; loop 2
	tlt	$p15, $t12, $t4		; loop 2
	lws	$t16, ($t13) L[2]		; loop 2
	lws	$t17, ($t14) L[3]		; loop 2
	add	$t18, $t16, $t17		; loop 2
	extsw	$t19, $t18		; loop 2
	add	$t20, $t10, $t19		; loop 2
	extsw	$t21, $t20		; loop 2
	addi	$t22, $t12, 1		; loop 2
	extsw	$t23, $t22		; loop 2
	addi	$t24, $t13, 4		; loop 2
	addi	$t25, $t14, 4		; loop 2
	tlt_t<$p15>	$p26, $t23, $t4		; loop 2
	mov_f<$p15>	$t27, $t13
	mov_f<$p15>	$t28, $t14
	mov_f<$p15>	$t29, $t12
	mov_f<$p15>	$t30, $t10
	bro_f<$p15>	reduce$3		; loop 2
	lws	$t31, ($t24) L[4]		; loop 2
	lws	$t32, ($t25) L[5]		; loop 2
	add	$t33, $t31, $t32		; loop 2
	extsw	$t34, $t33		; loop 2
	add	$t35, $t21, $t34		; loop 2
	extsw	$t36, $t35		; loop 2
	addi	$t37, $t23, 1		; loop 2
	extsw	$t38, $t37		; loop 2
	addi	$t39, $t24, 4		; loop 2
	addi	$t40, $t25, 4		; loop 2
	tlt_t<$p26>	$p41, $t38, $t4		; loop 2
	mov_f<$p26>	$t27, $t24
	mov_f<$p26>	$t28, $t25
	mov_f<$p26>	$t29, $t23
	mov_f<$p26>	$t30, $t21
	bro_f<$p26>	reduce$3		; loop 2
	lws	$t42, ($t39) L[6]		; loop 2
	lws	$t43, ($t40) L[7]		; loop 2
	add	$t44, $t42, $t43		; loop 2
	extsw	$t45, $t44		; loop 2
	add	$t46, $t36, $t45		; loop 2
	extsw	$t47, $t46		; loop 2
	addi	$t48, $t38, 1		; loop 2
	extsw	$t49, $t48		; loop 2
	addi	$t50, $t39, 4		; loop 2
	addi	$t51, $t40, 4		; loop 2
	tlt_t<$p41>	$p52, $t49, $t4		; loop 2
	mov_f<$p41>	$t27, $t39
	mov_f<$p41>	$t28, $t40
	mov_f<$p41>	$t29, $t38
	mov_f<$p41>	$t30, $t36
	bro_f<$p41>	reduce$3		; loop 2
	lws	$t53, ($t50) L[8]		; loop 2
	lws	$t54, ($t51) L[9]		; loop 2
	add	$t55, $t53, $t54		; loop 2
	extsw	$t56, $t55		; loop 2
	add	$t57, $t47, $t56		; loop 2
	extsw	$t58, $t57		; loop 2
	addi	$t59, $t49, 1		; loop 2
	extsw	$t60, $t59		; loop 2
	addi	$t61, $t50, 4		; loop 2
	addi	$t62, $t51, 4		; loop 2
	tlt_t<$p52>	$p63, $t60, $t4		; loop 2
	mov_f<$p52>	$t27, $t50
	mov_f<$p52>	$t28, $t51
	mov_f<$p52>	$t29, $t49
	mov_f<$p52>	$t30, $t47
	bro_f<$p52>	reduce$3		; loop 2
	mov_t<$p63>	$t27, $t61
	mov_t<$p63>	$t28, $t62
	mov_t<$p63>	$t29, $t60
	mov_t<$p63>	$t30, $t58
	bro_t<$p63>	reduce$7		; loop 2
	mov_f<$p63>	$t27, $t61
	mov_f<$p63>	$t28, $t62
	mov_f<$p63>	$t29, $t60
	mov_f<$p63>	$t30, $t58
	bro_f<$p63>	reduce$3		; loop 2
	write	$g70, $t30
	write	$g71, $t29
	write	$g72, $t28
	write	$g73, $t27
.bend
.bbegin reduce$8
	movi	$t0, 0
	bro	reduce$3		; loop 1
	write	$g70, $t0
.bend
.bbegin reduce$9
	movi	$t0, 0
	bro	reduce$4
	write	$g80, $t0
.bend

	.global	init_arrays
;VARIABLE "_ar0" size:8 $g70
;VARIABLE "_ar1" size:8 $g71
;VARIABLE "i_ssa_4" size:4 $g72
.bbegin init_arrays
	read	$t0, $g1
	addi	$t1, $t0, -32
	sd	($t1), $t0 S[0]
	entera	$t2, B
	entera	$t3, C
	movi	$t4, 0
	bro	init_arrays$1		; loop 3
	write	$g1, $t1
	write	$g70, $t2
	write	$g71, $t3
	write	$g72, $t4
.bend
.bbegin init_arrays$1
	read	$t0, $g70
	read	$t1, $g71
	movi	$t2, 1
	sw	($t0), $t2 S[0]		; loop 3
	sw	($t1), $t2 S[1]		; loop 3
	sw	4($t0), $t2 S[2]		; loop 3
	sw	4($t1), $t2 S[3]		; loop 3
	sw	8($t0), $t2 S[4]		; loop 3
	sw	8($t1), $t2 S[5]		; loop 3
	sw	12($t0), $t2 S[6]		; loop 3
	sw	12($t1), $t2 S[7]		; loop 3
	sw	16($t0), $t2 S[8]		; loop 3
	sw	16($t1), $t2 S[9]		; loop 3
	sw	20($t0), $t2 S[10]		; loop 3
	sw	20($t1), $t2 S[11]		; loop 3
	sw	24($t0), $t2 S[12]		; loop 3
	sw	24($t1), $t2 S[13]		; loop 3
	sw	28($t0), $t2 S[14]		; loop 3
	sw	28($t1), $t2 S[15]		; loop 3
	sw	32($t0), $t2 S[16]		; loop 3
	sw	32($t1), $t2 S[17]		; loop 3
	sw	36($t0), $t2 S[18]		; loop 3
	sw	36($t1), $t2 S[19]		; loop 3
	sw	40($t0), $t2 S[20]		; loop 3
	sw	40($t1), $t2 S[21]		; loop 3
	sw	44($t0), $t2 S[22]		; loop 3
	sw	44($t1), $t2 S[23]		; loop 3
	sw	48($t0), $t2 S[24]		; loop 3
	sw	48($t1), $t2 S[25]		; loop 3
	sw	52($t0), $t2 S[26]		; loop 3
	sw	52($t1), $t2 S[27]		; loop 3
	bro	init_arrays$2
.bend
.bbegin init_arrays$2
	read	$t0, $g70
	read	$t1, $g71
	read	$t2, $g72
	movi	$t3, 1
	sw	56($t0), $t3 S[0]		; loop 3
	sw	56($t1), $t3 S[1]		; loop 3
	sw	60($t0), $t3 S[2]		; loop 3
	sw	60($t1), $t3 S[3]		; loop 3
	sw	64($t0), $t3 S[4]		; loop 3
	sw	64($t1), $t3 S[5]		; loop 3
	sw	68($t0), $t3 S[6]		; loop 3
	sw	68($t1), $t3 S[7]		; loop 3
	sw	72($t0), $t3 S[8]		; loop 3
	sw	72($t1), $t3 S[9]		; loop 3
	sw	76($t0), $t3 S[10]		; loop 3
	sw	76($t1), $t3 S[11]		; loop 3
	addi	$t4, $t2, 20		; loop 3
	extsw	$t5, $t4		; loop 3
	addi	$t6, $t0, 80		; loop 3
	addi	$t7, $t1, 80		; loop 3
	addi	$t8, $t2, 20		; loop 3
	extsw	$t9, $t8		; loop 3
	tlti	$p10, $t9, 21		; loop 3
	bro_t<$p10>	init_arrays$1		; loop 3
	bro_f<$p10>	init_arrays$3		; loop 3
	write	$g70, $t6
	write	$g71, $t7
	write	$g72, $t5
.bend
.bbegin init_arrays$3
	read	$t0, $g1
	read	$t1, $g2
	addi	$t2, $t0, 32
	ret	$t1
	write	$g1, $t2
.bend

	.global	main
.bbegin main
	read	$t0, $g1
	read	$t1, $g2
	addi	$t2, $t0, -96
	sd	($t2), $t0 S[0]
	sd	8($t2), $t1 S[1]
	enterb	$t3, main$1
	callo	init_arrays
	write	$g1, $t2
	write	$g2, $t3
.bend
.bbegin main$1
	enterb	$t0, main$2
	callo	reduce
	write	$g2, $t0
.bend
.bbegin main$2
	read	$t0, $g3
	entera	$t1, _V0
	enterb	$t2, main$3
	callo	printf
	write	$g2, $t2
	write	$g3, $t1
	write	$g4, $t0
.bend
.bbegin main$3
	read	$t0, $g1
	ld	$t1, 8($t0) L[0]
	addi	$t2, $t0, 96
	ret	$t1
	write	$g1, $t2
	write	$g2, $t1
.bend




