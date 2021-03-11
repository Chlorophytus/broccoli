# Broccoli's Architecture
## Shaders
Each shader has 16 16-bit GPRs. Each ALU is 16-bit also. The Post-ALU does not
have to be used. Each shader has two Pre ALUs. Each shader also has a Post ALU
for modification of the two Pre ALUs' results.
