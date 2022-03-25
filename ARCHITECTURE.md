# Broccoli's Architecture

## TBD

Things will change frequently. This is just a brainstorming page...

## Display Controller

Up to 1280x720@60fps? Maybe.

## Display Lists

The Broccoli PPU functions with display lists as instructions of where to draw the texture cells.

## Texture Cells

The Broccoli picture processor is a 2D sprite-based powerhouse.

Each texture cell will compute X/Y pixel/coordinate data based on a matrix and a power-of-2 square (RTL defines it, 32x and 128x are examples) 8-bit pixel memory.

### Stencil testing

This is a forward-mapped rasterizer. **TODO: Explain this**

**TODO: Explain dynamic textures, where the stencil square can be dynamically changed.**
