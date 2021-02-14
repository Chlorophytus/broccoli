# broccoli
## About
A soft GPU prototype in Chisel 3

### Specifications
- 640x480@60Hz output
- CISC shaders

## Generating Verilog
A Scala App for generating Verilog has been included, runnable by `sbt`.
```shell
$ sbt run
```

## ISA
See [INSTRUCTION_SET.md](./INSTRUCTION_SET.md)