# Broccoli's Architecture

## **BULLETIN**: CURRENT TESTBED STATE

The testbed will currently display a 640x480 tiled texture of configurable size.

## TBD

Things will change frequently. This is just a brainstorming page...

## Framebuffer

The Broccoli PPU has two 320x240 framebuffers. 9-bit pixels with 3-bit channels.

## Texture Cells

The Broccoli picture processor is a 2D sprite-based powerhouse.

Each texture cell will compute X/Y pixel/coordinate data based on a matrix
and a power-of-2 square (RTL defines it, 32x and 128x are examples) 8-bit pixel
memory.

## Shaders (TBD)

The texture cells and write framebuffer can be manipulated procedurally by a
pixel equation. The specifics will be decided on later.
