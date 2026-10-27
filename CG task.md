# Computer Graphics Tasks 1, 2, and 3 Implementation

## Project context

WasteFlow renders its waste-collection map in the browser using the HTML Canvas 2D API. The Canvas displays procedurally textured roads and terrain, status lighting, locations, bins, planned routes, and an animated truck. This document maps the Computer Graphics Task 1, Task 2, and Task 3 concepts to the implementation.

## Task 1: Graphics setup and drawing algorithms

| Concept | Status | Implementation | Source location |
|---|---|---|---|
| Graphics environment setup | Partially implemented | The project initializes an HTML Canvas 2D rendering context. It does not create a native OpenGL/GLFW window or context. | [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx#L67), [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx#L80) |
| Simple graphics program | Implemented | Canvas renders the map background/grid, roads, planned route, location labels, bin markers, and truck. | [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx#L82), [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx#L105), [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx#L160) |
| Bresenham line drawing | Implemented | Integer raster line drawing is used for grid lines, roads, and planned route segments. Route segments can be dashed. | [graphicsAlgorithms.ts](frontend/src/graphicsAlgorithms.ts#L84), [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx#L99), [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx#L106) |
| Midpoint circle drawing | Implemented | The midpoint circle algorithm draws filled location/bin markers and bin marker outlines. | [graphicsAlgorithms.ts](frontend/src/graphicsAlgorithms.ts#L141), [graphicsAlgorithms.ts](frontend/src/graphicsAlgorithms.ts#L170), [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx#L163), [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx#L172) |
| OpenGL/GLFW setup and primitives | Not implemented | The application is a React browser app using Canvas 2D; it does not use OpenGL or GLFW. | — |

## Task 2: Transformations and clipping

| Concept | Status | Implementation | Source location |
|---|---|---|---|
| Translation | Implemented | The truck drawing origin is translated to the truck's interpolated position along the current route segment. | [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx#L144) |
| Rotation | Implemented | The truck is rotated according to the direction of travel between consecutive route locations. | [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx#L145) |
| Scaling / coordinate mapping | Implemented | Map coordinates are scaled to fit the current Canvas viewport. The drawing context is also scaled for device pixel ratio. | [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx#L80), [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx#L88) |
| Cohen–Sutherland line clipping | Implemented | Line segments are clipped to the Canvas viewport before Bresenham rasterization. This is used for grid, road, and route lines. | [graphicsAlgorithms.ts](frontend/src/graphicsAlgorithms.ts#L26), [graphicsAlgorithms.ts](frontend/src/graphicsAlgorithms.ts#L96), [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx#L99) |
| Redraw on viewport resize | Implemented | A `ResizeObserver` redraws the map when the Canvas size changes. | [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx#L180) |

## Task 3 (CO6): Texture mapping, lighting, and application integration

### Concepts and locations

| Task 3 concept | How it is used | Source location |
|---|---|---|
| Terrain surface texture | A repeating, procedurally generated Canvas texture gives the collection-area map a subtle ground surface. | [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx) |
| Road surface texture | Roads are clipped to the map viewport, rendered with shoulders and a repeating asphalt texture, and marked with dashed lane lines. | [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx), [graphicsAlgorithms.ts](frontend/src/graphicsAlgorithms.ts) |
| Bin and depot lighting | Radial light glows emphasize bin status and the depot; marker highlights add a simple shaded appearance. | [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx) |
| Truck lighting effect | A soft glow and directional headlight beam follow the animated truck's heading along the route. | [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx) |
| Integration in the application | Textured surfaces and lighting render as part of the existing route Canvas, alongside bins, roads, route animation, and controls; a map badge identifies the effects. | [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx), [animation.css](frontend/src/animation.css), [App.tsx](frontend/src/App.tsx) |
| Native 3D/UV texture mapping | Not implemented; the current effects are procedural 2D Canvas textures and gradients, not a 3D OpenGL scene. | [RouteCanvas.tsx](frontend/src/RouteCanvas.tsx) |

## Rendering flow

1. Obtain the Canvas 2D context and size the drawing buffer for the device pixel ratio.
2. Map model coordinates into Canvas viewport coordinates.
3. Fill the ground and road surfaces with repeating procedural textures; clip road strokes to the viewport.
4. Draw grid and planned-route lines using Bresenham rasterization, then apply status glows, depot lighting, and truck headlights.
5. Draw location and bin markers with the midpoint circle algorithm.
6. Translate and rotate the truck graphic, then redraw as route animation progresses.
7. Redraw the map when its viewport is resized.

## Summary

Task 1's simple graphics program, Bresenham line drawing, and midpoint circle drawing are implemented in the Canvas renderer. Graphics setup is browser Canvas 2D rather than OpenGL/GLFW. Task 2's translation, rotation, scaling, and Cohen–Sutherland line clipping are implemented. Task 3's procedural 2D terrain/road textures, status/depot lighting, and animated truck headlight are integrated into the map. Native OpenGL/GLFW and 3D texture mapping are not part of this React application.
