# Computer Graphics Tasks 1 and 2 Implementation

## Project context

WasteFlow renders its waste-collection map in the browser using the HTML Canvas 2D API. The Canvas displays roads, locations, bins, planned routes, and an animated truck. This document maps the Computer Graphics Task 1 and Task 2 concepts to the implementation.

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

## Rendering flow

1. Obtain the Canvas 2D context and size the drawing buffer for the device pixel ratio.
2. Map model coordinates into Canvas viewport coordinates.
3. Draw grid, roads, and planned route using Cohen–Sutherland clipping followed by Bresenham rasterization.
4. Draw location and bin markers with the midpoint circle algorithm.
5. Translate and rotate the truck graphic, then redraw as route animation progresses.
6. Redraw the map when its viewport is resized.

## Summary

Task 1's simple graphics program, Bresenham line drawing, and midpoint circle drawing are implemented in the Canvas renderer. Graphics setup is browser Canvas 2D rather than OpenGL/GLFW. Task 2's translation, rotation, scaling, and Cohen–Sutherland line clipping are implemented. Native OpenGL/GLFW is not part of this React application.
