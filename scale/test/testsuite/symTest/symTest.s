; .app-file "symTest.c"
.data	
.align	4
_V0:
.ascii	"%x\n\000"
.align	4
_V1:
.ascii	"%d\n\000"
.align	4
.global	a
a:
.space	4
.align	4
.global	b
b:
.int	0x1
.align	4
.global	c
c:
.int	b
.text	
.global	main
; Scheduler info: Fanout algorithm: Huffman with max mov3s
; Scheduler info: Scheduling algorithm: RBA

.bbegin main
;;;;;;;;;;; Begin read preamble
R[1] read G[1] N[7,1] N[0,0]
R[2] read G[2] N[10,1]
R[0] read G[12] N[8,1]
;;;;;;;;;;; End read preamble
N[0] <0> mov N[4,0] N[8,0]
N[4] <1> addi -112 N[5,0]
N[5] <2> mov N[6,0] W[1]
N[6] <3> mov N[7,0] N[10,0]
N[7] <4> sd S[0]
N[10] <5> sd S[1] 8
N[8] <6> sd S[2] -8
N[9] <7> genu %lo(_V0) N[13,0]
N[13] <8> app %bottom(_V0) W[3]
N[3] <9> genu %lo(c) N[2,0]
N[2] <10> app %bottom(c) N[1,0]
N[1] <11> lws L[3] W[0]
N[11] <12> genu %lo(main$1) N[15,0]
N[15] <13> app %bottom(main$1) W[2]
N[14] <14> callo B[0] printf
;;;;;;;;;;; Begin write epilogue
W[1] write G[1]
W[2] write G[2]
W[3] write G[3]
W[0] write G[4]
;;;;;;;;;;; End write epilogue
.bend

; Scheduler info: Fanout algorithm: Huffman with max mov3s
; Scheduler info: Scheduling algorithm: RBA

.bbegin main$1
;;;;;;;;;;; Begin read preamble
R[1] read G[1] N[1,0]
;;;;;;;;;;; End read preamble
N[5] <0> movi 4 N[4,1]
N[1] <1> addi 88 N[0,0]
N[0] <2> mov N[4,0] W[0]
N[4] <3> sw S[0]
N[3] <4> genu %lo(_V0) N[2,0]
N[2] <5> app %bottom(_V0) W[3]
N[6] <6> genu %lo(main$2) N[7,0]
N[7] <7> app %bottom(main$2) W[2]
N[9] <8> callo B[0] printf
;;;;;;;;;;; Begin write epilogue
W[2] write G[2]
W[3] write G[3]
W[0] write G[4]
;;;;;;;;;;; End write epilogue
.bend

; Scheduler info: Fanout algorithm: Huffman with max mov3s
; Scheduler info: Scheduling algorithm: RBA

.bbegin main$2
;;;;;;;;;;; Begin read preamble
;;;;;;;;;;; End read preamble
N[11] <0> genu %lo(b) N[7,0]
N[7] <1> app %bottom(b) N[3,0]
N[3] <2> mov N[2,0] W[0]
N[2] <3> mov N[6,0] N[1,0]
N[6] <4> lws L[0] W[4]
N[10] <5> movi 7 N[1,1]
N[1] <6> sw S[1]
N[0] <7> genu %lo(_V0) N[4,0]
N[4] <8> app %bottom(_V0) W[3]
N[5] <9> genu %lo(main$3) N[9,0]
N[9] <10> app %bottom(main$3) W[2]
N[8] <11> callo B[0] printf
;;;;;;;;;;; Begin write epilogue
W[2] write G[2]
W[3] write G[3]
W[0] write G[4]
W[4] write G[12]
;;;;;;;;;;; End write epilogue
.bend

; Scheduler info: Fanout algorithm: Huffman with max mov3s
; Scheduler info: Scheduling algorithm: RBA

.bbegin main$3
;;;;;;;;;;; Begin read preamble
;;;;;;;;;;; End read preamble
N[7] <0> genu %lo(_V1) N[6,0]
N[6] <1> app %bottom(_V1) W[3]
N[3] <2> genu %lo(b) N[2,0]
N[2] <3> app %bottom(b) N[1,0]
N[1] <4> lws L[0] W[0]
N[0] <5> genu %lo(main$4) N[4,0]
N[4] <6> app %bottom(main$4) W[2]
N[5] <7> callo B[0] printf
;;;;;;;;;;; Begin write epilogue
W[2] write G[2]
W[3] write G[3]
W[0] write G[4]
;;;;;;;;;;; End write epilogue
.bend

; Scheduler info: Fanout algorithm: Huffman with max mov3s
; Scheduler info: Scheduling algorithm: RBA

.bbegin main$4
;;;;;;;;;;; Begin read preamble
R[0] read G[12] N[12,0]
;;;;;;;;;;; End read preamble
N[12] <0> mov W[0]
N[3] <1> genu %lo(_V1) N[2,0]
N[2] <2> app %bottom(_V1) W[3]
N[1] <3> genu %lo(main$5) N[0,0]
N[0] <4> app %bottom(main$5) W[2]
N[5] <5> callo B[0] printf
;;;;;;;;;;; Begin write epilogue
W[2] write G[2]
W[3] write G[3]
W[0] write G[4]
;;;;;;;;;;; End write epilogue
.bend

; Scheduler info: Fanout algorithm: Huffman with max mov3s
; Scheduler info: Scheduling algorithm: RBA

.bbegin main$5
;;;;;;;;;;; Begin read preamble
R[1] read G[1] N[0,0] N[1,0]
;;;;;;;;;;; End read preamble
N[0] <0> ld L[0] 8 N[4,0]
N[4] <1> mov N[5,0] W[2]
N[1] <2> addi 112 N[2,0]
N[2] <3> mov N[3,0] W[1]
N[3] <4> ld L[1] -8 W[0]
N[5] <5> ret B[0]
;;;;;;;;;;; Begin write epilogue
W[1] write G[1]
W[2] write G[2]
W[0] write G[12]
;;;;;;;;;;; End write epilogue
.bend

