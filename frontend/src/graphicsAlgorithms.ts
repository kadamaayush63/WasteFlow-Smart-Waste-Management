export interface GraphicsPoint {
  x: number
  y: number
}

interface ClippedLine {
  start: GraphicsPoint
  end: GraphicsPoint
}

const INSIDE = 0
const LEFT = 1
const RIGHT = 2
const TOP = 4
const BOTTOM = 8

function outCode(point: GraphicsPoint, width: number, height: number) {
  let code = INSIDE
  if (point.x < 0) code |= LEFT
  else if (point.x > width) code |= RIGHT
  if (point.y < 0) code |= TOP
  else if (point.y > height) code |= BOTTOM
  return code
}

export function clipLineToViewport(
  start: GraphicsPoint,
  end: GraphicsPoint,
  width: number,
  height: number,
): ClippedLine | null {
  if (![start.x, start.y, end.x, end.y, width, height].every(Number.isFinite) || width < 0 || height < 0) {
    return null
  }

  let first = { ...start }
  let second = { ...end }
  let firstCode = outCode(first, width, height)
  let secondCode = outCode(second, width, height)

  while (true) {
    if ((firstCode | secondCode) === 0) return { start: first, end: second }
    if ((firstCode & secondCode) !== 0) return null

    const outsideCode = firstCode || secondCode
    let intersection: GraphicsPoint

    if (outsideCode & TOP) {
      if (second.y === first.y) return null
      intersection = {
        x: first.x + (second.x - first.x) * (0 - first.y) / (second.y - first.y),
        y: 0,
      }
    } else if (outsideCode & BOTTOM) {
      if (second.y === first.y) return null
      intersection = {
        x: first.x + (second.x - first.x) * (height - first.y) / (second.y - first.y),
        y: height,
      }
    } else if (outsideCode & RIGHT) {
      if (second.x === first.x) return null
      intersection = {
        x: width,
        y: first.y + (second.y - first.y) * (width - first.x) / (second.x - first.x),
      }
    } else {
      if (second.x === first.x) return null
      intersection = {
        x: 0,
        y: first.y + (second.y - first.y) * (0 - first.x) / (second.x - first.x),
      }
    }

    if (outsideCode === firstCode) {
      first = intersection
      firstCode = outCode(first, width, height)
    } else {
      second = intersection
      secondCode = outCode(second, width, height)
    }
  }
}

export function drawClippedRasterLine(
  context: CanvasRenderingContext2D,
  start: GraphicsPoint,
  end: GraphicsPoint,
  viewportWidth: number,
  viewportHeight: number,
  color: string,
  thickness = 1,
  dashPattern?: readonly number[],
) {
  const clipped = clipLineToViewport(start, end, viewportWidth, viewportHeight)
  if (!clipped) return

  let x = Math.round(clipped.start.x)
  let y = Math.round(clipped.start.y)
  const endX = Math.round(clipped.end.x)
  const endY = Math.round(clipped.end.y)
  const deltaX = Math.abs(endX - x)
  const stepX = x < endX ? 1 : -1
  const deltaY = -Math.abs(endY - y)
  const stepY = y < endY ? 1 : -1
  let error = deltaX + deltaY
  let step = 0
  const brushSize = Math.max(1, Math.round(thickness))
  const brushOffset = -Math.floor(brushSize / 2)
  const dashLength = dashPattern?.reduce((sum, length) => sum + length, 0) ?? 0

  context.fillStyle = color
  while (true) {
    const dashPosition = dashLength > 0 ? step % dashLength : 0
    let drawPixel = true
    if (dashPattern && dashLength > 0) {
      let patternPosition = dashPosition
      for (let index = 0; index < dashPattern.length; index += 1) {
        if (patternPosition < dashPattern[index]) {
          drawPixel = index % 2 === 0
          break
        }
        patternPosition -= dashPattern[index]
      }
    }
    if (drawPixel) context.fillRect(x + brushOffset, y + brushOffset, brushSize, brushSize)
    if (x === endX && y === endY) break

    const doubledError = 2 * error
    if (doubledError >= deltaY) {
      error += deltaY
      x += stepX
    }
    if (doubledError <= deltaX) {
      error += deltaX
      y += stepY
    }
    step += 1
  }
}

export function drawMidpointFilledCircle(
  context: CanvasRenderingContext2D,
  center: GraphicsPoint,
  radius: number,
  color: string,
) {
  const centerX = Math.round(center.x)
  const centerY = Math.round(center.y)
  const circleRadius = Math.max(0, Math.round(radius))
  let x = circleRadius
  let y = 0
  let decision = 1 - circleRadius

  context.fillStyle = color
  while (x >= y) {
    context.fillRect(centerX - x, centerY + y, 2 * x + 1, 1)
    context.fillRect(centerX - x, centerY - y, 2 * x + 1, 1)
    context.fillRect(centerX - y, centerY + x, 2 * y + 1, 1)
    context.fillRect(centerX - y, centerY - x, 2 * y + 1, 1)
    y += 1
    if (decision < 0) {
      decision += 2 * y + 1
    } else {
      x -= 1
      decision += 2 * (y - x) + 1
    }
  }
}

export function drawMidpointCircleOutline(
  context: CanvasRenderingContext2D,
  center: GraphicsPoint,
  radius: number,
  color: string,
) {
  const centerX = Math.round(center.x)
  const centerY = Math.round(center.y)
  const circleRadius = Math.max(0, Math.round(radius))
  let x = circleRadius
  let y = 0
  let decision = 1 - circleRadius

  context.fillStyle = color
  while (x >= y) {
    context.fillRect(centerX + x, centerY + y, 1, 1)
    context.fillRect(centerX + y, centerY + x, 1, 1)
    context.fillRect(centerX - y, centerY + x, 1, 1)
    context.fillRect(centerX - x, centerY + y, 1, 1)
    context.fillRect(centerX - x, centerY - y, 1, 1)
    context.fillRect(centerX - y, centerY - x, 1, 1)
    context.fillRect(centerX + y, centerY - x, 1, 1)
    context.fillRect(centerX + x, centerY - y, 1, 1)
    y += 1
    if (decision < 0) {
      decision += 2 * y + 1
    } else {
      x -= 1
      decision += 2 * (y - x) + 1
    }
  }
}
