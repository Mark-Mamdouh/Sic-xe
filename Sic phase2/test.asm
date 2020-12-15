COPY     START   1000               
FIRST    STL     RETADR
CLOOP    JSUB    RDREC
         LDA     LENGTH
         COMP    =X'00'
         JEQ	 ENDFIL
    	 JSUB    WRREC
    	 J       CLOOP
ENDFIL   LDA     =C'EOF'
    	 STA     BUFFER
    	 LDA     THREE
    	 STA     LENGTH
    	 JSUB    WRREC
    	 LDL     RETADR
    	 RSUB
THREE    WORD    3
RETADR   EQU     *
LENGTH   RESW    1
BUFFER   RESW    4096
RDREC    LDX     =X'00'
         LDA     =X'00'
RLOOP	 TD      INPUT
         JEQ     RLOOP
    	 RD      INPUT
    	 COMP    =X'00'
    	 JEQ     EXIT
    	 STCH    BUFFER,X
    	 TIX     MAXLEN
    	 JLT     RLOOP
EXIT     STX     LENGTH
         RSUB
         ORG     RLOOP-RDREC
INPUT    BYTE    X'F1'
MAXLEN   WORD    4096
WRREC    LDX     =X'00'
WLOOP    TD      =X'05'
    	 JEQ     WLOOP
    	 LDCH    BUFFER,X
    	 WD      =X'05'
    	 TIX     LENGTH
    	 JLT     WLOOP
    	 RSUB
         ORG     WLOOP+5
TEXT     EQU     RETADR		 
         END     FIRST