Prbn08   START   1000  
         LDA     ZERO
         STA     INDEX
LOOP     LDA     ZERO
         LDX     INDEX
         STA     ALPHA,X
         LDA     INDEX
         ADD     THREE
         STA     INDEX
         COMP    K300
         JLT     LOOP 		 
K300     WORD    300
ZERO     WORD    0
THREE    WORD    3
INDEX    RESW    1
ALPHA    RESW    100
         END     Prbn08
