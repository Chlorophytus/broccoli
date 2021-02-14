# broccoli
## Instruction set
All opcodes that are unlisted are unimplemented and will cause a break. 
### Addition
#### `10h` - Add next word to GPR0
#### `11h` - Add next word to GPR1
#### `12h` - Add next word to GPR2
#### `13h` - Add next word to GPR3
#### `14h` - Add next word to GPR4
#### `15h` - Add next word to GPR5
#### `16h` - Add next word to GPR6
#### `17h` - Add next word to GPR7
### Subtraction
#### `18h` - Subtract next word to GPR0
#### `19h` - Subtract next word to GPR1
#### `1Ah` - Subtract next word to GPR2
#### `1Bh` - Subtract next word to GPR3
#### `1Ch` - Subtract next word to GPR4
#### `1Dh` - Subtract next word to GPR5
#### `1Eh` - Subtract next word to GPR6
#### `1Fh` - Subtract next word to GPR7
### Saturated Multiplication
#### `30h` - Multiply GPR0 by next word, saturating
#### `31h` - Multiply GPR1 by next word, saturating
#### `32h` - Multiply GPR2 by next word, saturating
#### `33h` - Multiply GPR3 by next word, saturating
#### `34h` - Multiply GPR4 by next word, saturating
#### `35h` - Multiply GPR5 by next word, saturating
#### `36h` - Multiply GPR6 by next word, saturating
#### `37h` - Multiply GPR7 by next word, saturating
### Division
Dividing by zero asserts the Div0 flag.
#### `38h` - Divide GPR0 by next word
#### `39h` - Divide GPR1 by next word
#### `3Ah` - Divide GPR2 by next word
#### `3Bh` - Divide GPR3 by next word
#### `3Ch` - Divide GPR4 by next word
#### `3Dh` - Divide GPR5 by next word
#### `3Eh` - Divide GPR6 by next word
#### `3Fh` - Divide GPR7 by next word
### Rotate
Rotates rightward. A mask will be defined by a word or GPR.
#### `40h` - Rotate Right
```
@[n + 0] = 0x40 // rotate right...
@[n + 1] = 0b0011_0001 // GPR1 (low nybble) by 3 (high nybble)
```
#### `41h` - AND Masked Rotate Right
```
@[n + 0] = 0x41 // rotate right...
@[n + 1] = 0b0011_0001 // GPR1 (low nybble) by 3 (high nybble)
@[n + 2] = 0x7F // then AND mask hibyte by 0x7F
@[n + 3] = 0xFF // then AND mask lobyte by 0xFF
```
#### `42h` - XOR Masked Rotate Right
```
@[n + 0] = 0x41 // rotate right...
@[n + 1] = 0b0011_0001 // GPR1 (low nybble) by 3 (high nybble)
@[n + 2] = 0x7F // then AND mask hibyte by 0x7F
@[n + 3] = 0xFF // then AND mask lobyte by 0xFF
```

#### `43h` - OR Masked Rotate Right
```
@[n + 0] = 0x41 // rotate right...
@[n + 1] = 0b0011_0001 // GPR1 (low nybble) by 3 (high nybble)
@[n + 2] = 0x7F // then OR mask hibyte by 0x7F
@[n + 3] = 0xFF // then OR mask lobyte by 0xFF
```
#### `48h` - Rotate Right Mask By GPR0
```
@[n + 0] = 0x48 // rotate right...
//                   /= AND
//                  //= OR
//                 ///= XOR
@[n + 1] = 0b0011_0001 // XOR (low nybble) but pre-shift by 3 (high nybble)
```
#### `49h` - Rotate Right Mask By GPR1
SEE: `48h`
#### `4Ah` - Rotate Right Mask By GPR2
SEE: `48h`
#### `4Bh` - Rotate Right Mask By GPR3
SEE: `48h`
#### `4Ch` - Rotate Right Mask By GPR4
SEE: `48h`
#### `4Dh` - Rotate Right Mask By GPR5
SEE: `48h`
#### `4Eh` - Rotate Right Mask By GPR6
SEE: `48h`
#### `4Fh` - Rotate Right Mask By GPR7
SEE: `48h`
### Logical AND
#### `50h` - AND GPR0 by next word
#### `51h` - AND GPR1 by next word
#### `52h` - AND GPR2 by next word
#### `53h` - AND GPR3 by next word
#### `54h` - AND GPR4 by next word
#### `55h` - AND GPR5 by next word
#### `56h` - AND GPR6 by next word
#### `57h` - AND GPR7 by next word
### Logical OR
#### `58h` - OR GPR0 by next word
#### `59h` - OR GPR1 by next word
#### `5Ah` - OR GPR2 by next word
#### `5Bh` - OR GPR3 by next word
#### `5Ch` - OR GPR4 by next word
#### `5Dh` - OR GPR5 by next word
#### `5Eh` - OR GPR6 by next word
#### `5Fh` - OR GPR7 by next word
### Logical XOR
#### `60h` - XOR GPR0 by next word
#### `61h` - XOR GPR1 by next word
#### `62h` - XOR GPR2 by next word
#### `63h` - XOR GPR3 by next word
#### `64h` - XOR GPR4 by next word
#### `65h` - XOR GPR5 by next word
#### `66h` - XOR GPR6 by next word
#### `67h` - XOR GPR7 by next word
### Load
#### `70h` - Load GPR0 with next word
#### `71h` - Load GPR1 with next word
#### `72h` - Load GPR2 with next word
#### `73h` - Load GPR3 with next word
#### `74h` - Load GPR4 with next word
#### `75h` - Load GPR5 with next word
#### `76h` - Load GPR6 with next word
#### `77h` - Load GPR7 with next word
### Store
#### `78h` - Store GPR0 with next word as address
#### `79h` - Store GPR1 with next word as address
#### `7Ah` - Store GPR2 with next word as address
#### `7Bh` - Store GPR3 with next word as address
#### `7Ch` - Store GPR4 with next word as address
#### `7Dh` - Store GPR5 with next word as address
#### `7Eh` - Store GPR6 with next word as address
#### `7Fh` - Store GPR7 with next word as address
### Load X/Y
#### `80h` - Load GPR0 with **X**
#### `81h` - Load GPR1 with **X**
#### `82h` - Load GPR2 with **X**
#### `83h` - Load GPR3 with **X**
#### `84h` - Load GPR4 with **X**
#### `85h` - Load GPR5 with **X**
#### `86h` - Load GPR6 with **X**
#### `87h` - Load GPR7 with **X**
#### `88h` - Load GPR0 with **Y**
#### `89h` - Load GPR1 with **Y**
#### `8Ah` - Load GPR2 with **Y**
#### `8Bh` - Load GPR3 with **Y**
#### `8Ch` - Load GPR4 with **Y**
#### `8Dh` - Load GPR5 with **Y**
#### `8Eh` - Load GPR6 with **Y**
#### `8Fh` - Load GPR7 with **Y**
### Swizzle Pixel Data
#### `90h` - Swizzle pixel info LO
```
@[n + 0] = 0x90 // swizzle store (LO byte of GPR only) pixelinfo of

               // ABGR
               // ||||
@[n + 1] = 0b0110_0110 // GPR6 (lo nybble) to BLU and GRN (high nybble)
```
#### `98h` - Swizzle pixel info HI
```
@[n + 0] = 0x98 // swizzle store (HI byte of GPR only) pixelinfo of

               // ABGR
               // ||||
@[n + 1] = 0b0001_0001 // GPR1 (lo nybble) to RED (high nybble)
```
### Absolute Jumps
#### `C0h` - Absolute Jump if **X** = GPR0
#### `C1h` - Absolute Jump if **X** = GPR1
#### `C2h` - Absolute Jump if **X** = GPR2
#### `C3h` - Absolute Jump if **X** = GPR3
#### `C4h` - Absolute Jump if **X** = GPR4
#### `C5h` - Absolute Jump if **X** = GPR5
#### `C6h` - Absolute Jump if **X** = GPR6
#### `C7h` - Absolute Jump if **X** = GPR7
#### `D0h` - Absolute Jump if **Y** = GPR0
#### `D1h` - Absolute Jump if **Y** = GPR1
#### `D2h` - Absolute Jump if **Y** = GPR2
#### `D3h` - Absolute Jump if **Y** = GPR3
#### `D4h` - Absolute Jump if **Y** = GPR4
#### `D5h` - Absolute Jump if **Y** = GPR5
#### `D6h` - Absolute Jump if **Y** = GPR6
#### `D7h` - Absolute Jump if **Y** = GPR7
### Relative Jumps
#### `C8h` - Relative Jump if **X** = GPR0
#### `C9h` - Relative Jump if **X** = GPR1
#### `CAh` - Relative Jump if **X** = GPR2
#### `CBh` - Relative Jump if **X** = GPR3
#### `CCh` - Relative Jump if **X** = GPR4
#### `CDh` - Relative Jump if **X** = GPR5
#### `CEh` - Relative Jump if **X** = GPR6
#### `CFh` - Relative Jump if **X** = GPR7
#### `D8h` - Relative Jump if **Y** = GPR0
#### `D9h` - Relative Jump if **Y** = GPR1
#### `DAh` - Relative Jump if **Y** = GPR2
#### `DBh` - Relative Jump if **Y** = GPR3
#### `DCh` - Relative Jump if **Y** = GPR4
#### `DDh` - Relative Jump if **Y** = GPR5
#### `DEh` - Relative Jump if **Y** = GPR6
#### `DFh` - Relative Jump if **Y** = GPR7
### Indirect Jumps
#### `E0h` - Indirect Jump via GPR0
#### `E1h` - Indirect Jump via GPR1
#### `E2h` - Indirect Jump via GPR2
#### `E3h` - Indirect Jump via GPR3
#### `E4h` - Indirect Jump via GPR4
#### `E5h` - Indirect Jump via GPR5
#### `E6h` - Indirect Jump via GPR6
#### `E7h` - Indirect Jump via GPR7
#### `E8h` - Set Interrupt Mask to GPR0
#### `E9h` - Set Interrupt Mask to GPR1
#### `EAh` - Set Interrupt Mask to GPR2
#### `EBh` - Set Interrupt Mask to GPR3
#### `ECh` - Set Interrupt Mask to GPR4
#### `EDh` - Set Interrupt Mask to GPR5
#### `EEh` - Set Interrupt Mask to GPR6
#### `EFh` - Set Interrupt Mask to GPR7
### Miscellaneous
#### `F0h` - No op
#### `F1h` - Clear Zero Flag
#### `F2h` - Clear Sign Flag
#### `F3h` - Clear DivZ Flag
#### `F4h` - Clear SatP Flag
#### `F5h` - Clear SatN Flag
#### `F6h` - No op
#### `F7h` - No op
#### `F8h` - Set Interrupt Mask To Next Word
#### `FDh` - No op
#### `FEh` - Load VBIOS/Reset
#### `FFh` - Break
