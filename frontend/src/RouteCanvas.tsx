import { Pause, Play, RotateCcw } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { clipLineToViewport, drawClippedRasterLine, drawMidpointCircleOutline, drawMidpointFilledCircle } from './graphicsAlgorithms'
import type { Bin, Location, Road, Route } from './types'

interface RouteCanvasProps {
  locations: Location[]
  roads: Road[]
  bins: Bin[]
  route: Route | null
  truckLocationId: number | null
}

const palette = {
  ink: '#17221f',
  grid: '#d7d6ca',
  lime: '#c8f45b',
  coral: '#f2765f',
  paper: '#f7f5ed',
  blue: '#5c91a8',
}

function createTextureTile(surface: 'terrain' | 'road') {
  const tile = document.createElement('canvas')
  tile.width = 64
  tile.height = 64
  const context = tile.getContext('2d')
  if (!context) return tile

  context.fillStyle = surface === 'terrain' ? '#f2f1e6' : '#a9aea5'
  context.fillRect(0, 0, tile.width, tile.height)

  let seed = surface === 'terrain' ? 7919 : 104729
  const random = () => {
    seed = (seed * 16807) % 2147483647
    return (seed - 1) / 2147483646
  }
  for (let index = 0; index < 56; index += 1) {
    const x = random() * tile.width
    const y = random() * tile.height
    const radius = surface === 'terrain' ? 1 + random() * 3 : 0.6 + random() * 1.2
    context.fillStyle = surface === 'terrain'
      ? index % 2 === 0 ? 'rgba(125, 151, 94, 0.13)' : 'rgba(189, 166, 119, 0.12)'
      : index % 2 === 0 ? 'rgba(236, 239, 230, 0.32)' : 'rgba(69, 78, 70, 0.2)'
    context.beginPath()
    context.ellipse(x, y, radius * 1.5, radius, random() * Math.PI, 0, Math.PI * 2)
    context.fill()
  }
  return tile
}

function drawLightGlow(
  context: CanvasRenderingContext2D,
  x: number,
  y: number,
  radius: number,
  color: string,
) {
  const glow = context.createRadialGradient(x, y, 0, x, y, radius)
  glow.addColorStop(0, color)
  glow.addColorStop(1, 'rgba(255, 255, 255, 0)')
  context.fillStyle = glow
  context.beginPath()
  context.arc(x, y, radius, 0, Math.PI * 2)
  context.fill()
}

export function RouteCanvas({ locations, roads, bins, route, truckLocationId }: RouteCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const animationFrame = useRef<number | null>(null)
  const startedAt = useRef(0)
  const pausedAt = useRef(0)
  const textureTiles = useMemo(() => ({
    terrain: createTextureTile('terrain'),
    road: createTextureTile('road'),
  }), [])
  const [progress, setProgress] = useState(0)
  const [isPlaying, setIsPlaying] = useState(false)

  const routeLocations = useMemo(() => {
    const start = locations.find((location) => location.id === truckLocationId)
    const stops = route?.stops.map((stop) => locations.find((location) => location.id === stop.locationId)).filter((location): location is Location => Boolean(location)) ?? []
    return start ? [start, ...stops] : stops
  }, [locations, route, truckLocationId])

  const stopCount = Math.max(0, routeLocations.length - 1)

  useEffect(() => {
    setProgress(0)
    setIsPlaying(route?.status === 'COLLECTING')
    pausedAt.current = 0
  }, [route?.id, route?.status])

  useEffect(() => {
    if (!isPlaying || stopCount === 0) return
    const duration = Math.max(2600, stopCount * 2200)
    startedAt.current = performance.now() - pausedAt.current * duration
    const animate = (now: number) => {
      const nextProgress = Math.min(1, (now - startedAt.current) / duration)
      setProgress(nextProgress)
      if (nextProgress < 1) animationFrame.current = requestAnimationFrame(animate)
      else { setIsPlaying(false); pausedAt.current = 1 }
    }
    animationFrame.current = requestAnimationFrame(animate)
    return () => { if (animationFrame.current !== null) cancelAnimationFrame(animationFrame.current) }
  }, [isPlaying, stopCount])

  const togglePlayback = () => {
    if (progress >= 1) { pausedAt.current = 0; setProgress(0) }
    else pausedAt.current = progress
    setIsPlaying((playing) => !playing)
  }

  const replay = () => { pausedAt.current = 0; setProgress(0); setIsPlaying(true) }

  useEffect(() => {
    const render = () => {
      const canvas = canvasRef.current
      if (!canvas) return
      const context = canvas.getContext('2d')
      if (!context) return
      const viewWidth = canvas.clientWidth
      const viewHeight = canvas.clientHeight
      if (viewWidth <= 0 || viewHeight <= 0) return

      const pixelRatio = window.devicePixelRatio || 1
      canvas.width = Math.round(viewWidth * pixelRatio)
      canvas.height = Math.round(viewHeight * pixelRatio)
      context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)
      context.fillStyle = context.createPattern(textureTiles.terrain, 'repeat') ?? palette.paper
      context.fillRect(0, 0, viewWidth, viewHeight)
      const roadTexture = context.createPattern(textureTiles.road, 'repeat')

      const minX = Math.min(...locations.map((location) => location.xCoordinate), 0)
      const maxX = Math.max(...locations.map((location) => location.xCoordinate), 100)
      const minY = Math.min(...locations.map((location) => location.yCoordinate), 0)
      const maxY = Math.max(...locations.map((location) => location.yCoordinate), 100)
      const scale = Math.min((viewWidth - 64) / (maxX - minX), (viewHeight - 64) / (maxY - minY))
      const point = (locationId: number) => {
        const location = locations.find((item) => item.id === locationId)
        if (!location) return { x: viewWidth / 2, y: viewHeight / 2 }
        return {
          x: 32 + (location.xCoordinate - minX) * scale,
          y: 32 + (location.yCoordinate - minY) * scale,
        }
      }

      for (let x = 32; x < viewWidth; x += 32) {
        drawClippedRasterLine(context, { x, y: 0 }, { x, y: viewHeight }, viewWidth, viewHeight, palette.grid)
      }
      for (let y = 32; y < viewHeight; y += 32) {
        drawClippedRasterLine(context, { x: 0, y }, { x: viewWidth, y }, viewWidth, viewHeight, palette.grid)
      }

      roads.forEach((road) => {
        const start = point(road.sourceLocationId)
        const end = point(road.destinationLocationId)
        drawClippedRasterLine(
          context,
          start,
          end,
          viewWidth,
          viewHeight,
          '#858b82',
          14,
        )
        const clipped = clipLineToViewport(start, end, viewWidth, viewHeight)
        if (!clipped) return
        context.save()
        context.lineCap = 'round'
        context.beginPath()
        context.moveTo(clipped.start.x, clipped.start.y)
        context.lineTo(clipped.end.x, clipped.end.y)
        context.strokeStyle = '#d5d0bf'
        context.lineWidth = 14
        context.stroke()
        context.strokeStyle = roadTexture ?? '#a9aea5'
        context.lineWidth = 10
        context.stroke()
        context.strokeStyle = 'rgba(247, 245, 237, 0.72)'
        context.lineWidth = 1
        context.setLineDash([5, 8])
        context.stroke()
        context.restore()
      })

      const stopLocations = routeLocations.map((location) => location.id)
      for (let index = 0; index < stopLocations.length - 1; index += 1) {
        drawClippedRasterLine(
          context,
          point(stopLocations[index]),
          point(stopLocations[index + 1]),
          viewWidth,
          viewHeight,
          palette.ink,
          5,
          [9, 7],
        )
      }

      if (routeLocations.length > 1) {
        const segmentProgress = progress * (routeLocations.length - 1)
        const segmentIndex = Math.max(0, Math.min(routeLocations.length - 2, Math.floor(segmentProgress)))
        const localProgress = Math.min(1, segmentProgress - segmentIndex)
        const startLocation = routeLocations[segmentIndex]
        const endLocation = routeLocations[segmentIndex + 1]
        if (!startLocation || !endLocation) return
        const start = point(startLocation.id)
        const end = point(endLocation.id)
        const truckX = start.x + (end.x - start.x) * localProgress
        const truckY = start.y + (end.y - start.y) * localProgress
        const heading = Math.atan2(end.y - start.y, end.x - start.x)
        context.save()
        context.translate(truckX, truckY)
        context.rotate(heading)
        drawLightGlow(context, 0, 0, 27, 'rgba(242, 118, 95, 0.24)')
        const headlight = context.createLinearGradient(11, 0, 84, 0)
        headlight.addColorStop(0, 'rgba(255, 238, 157, 0.44)')
        headlight.addColorStop(1, 'rgba(255, 238, 157, 0)')
        context.fillStyle = headlight
        context.beginPath()
        context.moveTo(11, -5)
        context.lineTo(84, -25)
        context.lineTo(84, 25)
        context.lineTo(11, 5)
        context.closePath()
        context.fill()
        context.fillStyle = palette.coral
        context.strokeStyle = palette.paper
        context.lineWidth = 3
        context.beginPath()
        context.roundRect(-14, -9, 24, 18, 4)
        context.fill(); context.stroke()
        context.fillStyle = palette.ink
        context.beginPath(); context.roundRect(5, -7, 9, 14, 2); context.fill()
        context.fillStyle = palette.ink
        context.beginPath(); context.arc(-7, 10, 3, 0, Math.PI * 2); context.fill()
        context.beginPath(); context.arc(8, 10, 3, 0, Math.PI * 2); context.fill()
        context.restore()
      }

      locations.forEach((location) => {
        const current = point(location.id)
        const isDepot = location.name.toLowerCase().includes('depot')
        if (isDepot) {
          drawLightGlow(context, current.x, current.y, 31, 'rgba(200, 244, 91, 0.34)')
        }
        drawMidpointFilledCircle(context, current, isDepot ? 8 : 5, isDepot ? palette.lime : palette.ink)
        context.fillStyle = 'rgba(255, 255, 255, 0.72)'
        context.beginPath()
        context.arc(current.x - 2, current.y - 2, isDepot ? 2.5 : 1.5, 0, Math.PI * 2)
        context.fill()
        context.fillStyle = palette.ink
        context.font = '11px Space Mono, monospace'
        context.fillText(location.name, current.x + 9, current.y - 9)
      })

      bins.forEach((bin) => {
        const current = point(bin.locationId)
        const color = bin.status === 'CRITICAL' ? palette.coral : bin.status === 'HIGH' ? '#f3bd57' : palette.blue
        const glowColor = bin.status === 'CRITICAL'
          ? 'rgba(242, 118, 95, 0.34)'
          : bin.status === 'HIGH'
            ? 'rgba(243, 189, 87, 0.32)'
            : 'rgba(92, 145, 168, 0.24)'
        drawLightGlow(context, current.x, current.y, 24, glowColor)
        drawMidpointFilledCircle(context, current, 9, color)
        drawMidpointCircleOutline(context, current, 10, palette.paper)
        context.fillStyle = 'rgba(255, 255, 255, 0.76)'
        context.beginPath()
        context.arc(current.x - 3, current.y - 3, 2.5, 0, Math.PI * 2)
        context.fill()
      })
    }

    render()
    const canvas = canvasRef.current
    if (!canvas) return
    const resizeObserver = new ResizeObserver(render)
    resizeObserver.observe(canvas)
    return () => resizeObserver.disconnect()
  }, [bins, locations, roads, progress, route, routeLocations, textureTiles])

  const currentStop = stopCount === 0 ? 0 : Math.min(stopCount, Math.floor(progress * stopCount) + 1)
  return <div className="route-canvas-stage"><canvas ref={canvasRef} className="route-canvas" aria-label="Textured waste collection map with status lighting and animated truck headlights" /><span className="graphics-effects-label">TEXTURED MAP · STATUS LIGHTING</span><div className="animation-controls"><button className="animation-button" onClick={togglePlayback} title={isPlaying ? 'Pause truck animation' : 'Play truck animation'}>{isPlaying ? <Pause size={14} /> : <Play size={14} />}</button><button className="animation-button" onClick={replay} title="Replay truck animation"><RotateCcw size={14} /></button><span><b>{route ? (progress >= 1 ? 'Route complete' : `Truck moving · stop ${currentStop} of ${stopCount}`) : 'No route selected'}</b><small>{route ? `${Math.round(progress * 100)}% of optimal sequence` : 'Build a route to animate'}</small></span><div className="animation-progress"><span style={{ width: `${progress * 100}%` }} /></div></div></div>
}
