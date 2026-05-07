# Landing Page Scroll Reveal Animation — Design Document

**Date:** 2026-05-08
**Status:** Approved
**Scope:** Single-image scroll-driven hero animation with block-grid mask reveal, replacing the static LandingHero.vue

## Goal

Replace the current static `LandingHero.vue` with an inversa.com-style scroll-driven hero animation. The core mechanic: a single background image with a block-grid SVG mask that scales inward on scroll to create an erosion/reveal effect, plus grid lines, indicator dots, text slides, and a progress bar.

## What Stays vs Changes

**Stays:** LandingHeader, LandingLogin, LandingFooter, HomeView scroll container, color scheme, fonts.

**Changes:** `LandingHero.vue` — full rewrite from static image+text to scroll-driven animated layers.

## Design Reference

inversa.com hero section — block-grid mask erosion from edges, reverse parallax background, coordinate grid overlay, indicator dots, vertical progress bar, Lenis smooth scrolling.

## Page Structure

```
HomeView.vue (Lenis scroll container, ~450svh total)
├── LandingHeader.vue        — Fixed top nav (unchanged)
├── LandingHero.vue          — Hero animation (~350svh) ← REWRITE
│   ├── <img>                — Background image, reverse parallax Y translate
│   ├── <svg mask>           — Block-grid mask, scale animation
│   ├── <div grid>           — Coordinate grid lines, opacity animation
│   ├── <div dots>           — Indicator dots (yellow/red/green), opacity animation
│   ├── <div slides>         — 4 text content slides, opacity animation
│   ├── <div progress>       — Right-side vertical progress bar
│   └── <div cursor>         — Custom cursor (desktop only)
├── LandingLogin.vue         — Login section (unchanged)
└── LandingFooter.vue        — Footer (unchanged)
```

## Layer Stacking (z-index)

| z-index | Layer | Description |
|---------|-------|-------------|
| 0 | Background image | Ocean image, GSAP yPercent parallax |
| 1 | SVG mask overlay | Block-grid mask (black rects from edges), scale via GSAP |
| 2 | Grid overlay | Coordinate grid lines, opacity via GSAP |
| 3 | Indicator dots | Yellow/red/green circles, opacity via GSAP |
| 4 | Text slides | 4 slides with headings + body text |
| 10 | Progress bar | Right-side vertical line, scaleY fill |
| 50 | Fixed header | Top navigation (unchanged) |
| 99 | Custom cursor | Circle cursor with ring (desktop only, optional) |

## Scroll Animation Timeline

Hero height: ~350svh. All animations driven by GSAP ScrollTrigger with `scrub: true`.

| Scroll Progress | Phase | Effects |
|----------------|-------|---------|
| 0%–25% | **Full Image** | Mask scale = 1.0 (mask outside viewport, full image visible). Progress bar starts filling. Background image reverse parallax begins. |
| 25%–40% | **Mask Erosion** | Mask scale 1.0 → 0.92. Block rectangles erode in from 4 edges. Grid lines + indicator dots fade in (opacity 0 → 1). Text slides 2-3-4 cycle. |
| 40%–60% | **Mask Recovery** | Mask scale 0.92 → 1.0. Block rectangles retract. Grid lines + indicator dots fade out (opacity 1 → 0). |
| 60%–100% | **Fade Out** | Hero content fades out. Login section scrolls into view. |

**Reverse parallax:** Background image `yPercent` transitions from 0 → -15 over the full scroll range (scroll down → image shifts up → user sees upper portion of image).

## SVG Mask Design: Block Grid

The mask is composed of **rectangular blocks** arranged along the 4 edges of the viewport, forming an irregular grid-erosion border. The center area is open (transparent), revealing the image.

**Structure:**
- Single `<svg>` element filling the hero container
- Background layer: full-size `<rect>` filled with `#13140e` (page background)
- Mask applied to a white `<rect>` that represents the visible image area
- The mask is defined by ~40-60 `<rect>` elements of varying sizes positioned along top/bottom/left/right edges
- Some rectangles extend further inward than others, creating an irregular, blocky erosion pattern

**Key characteristics:**
- Rectangles are axis-aligned (not rotated), varying in width (20px–120px) and height (15px–80px)
- Distribution is denser near edges, sparser toward center
- Some blocks "reach" further into the image than others
- The overall shape resembles a rough, blocky cave opening

**Scale animation mechanic:**
- The SVG mask layer is wrapped in a container with `transform-origin: center center`
- GSAP animates `scale` from 1.0 → 0.92 → 1.0
- When scale < 1.0, the edge blocks visually move inward, creating the erosion effect
- When scale returns to 1.0, blocks retreat back to edges

## Grid Lines

Horizontal and vertical lines at regular intervals (`stroke: #404040`, `stroke-width: 0.5`). Opacity controlled by GSAP (fades in during erosion, fades out during recovery).

## Indicator Dots

5-7 small circles (`r: 2-5px`) positioned at scattered coordinates on the image. Colors: `#ebfc72` (yellow), `#f1664d` (red), `#00d399` (green). Opacity controlled by GSAP in sync with grid lines.

## Progress Bar

Vertical bar on the right edge of the hero. Background: `#404040` line. Fill: `#ebfc72` (yellow) animates from top to bottom via `scaleY` (0 → 1), mapping to scroll progress 0% → 100%.

## Text Slides

4 slides overlaid on the image, positioned at different vertical offsets. Content changes via opacity crossfade as user scrolls. Existing content preserved from current `LandingHero.vue`.

## Custom Cursor

Desktop only (hidden on touch devices). Circle cursor with scroll-progress ring. Follows mouse position via `mousemove` event. Optional — can be deferred if implementation is complex.

## Technical Architecture

### Dependencies (already installed)
```
gsap  ^3.15.0   — ScrollTrigger + core animation engine
lenis ^1.3.23   — Smooth inertial scrolling
```

### Lenis Integration

- Lenis instance created in `HomeView.vue` mounted hook
- `raf` loop drives Lenis via `requestAnimationFrame`
- GSAP ScrollTrigger synchronized with Lenis via `ScrollTrigger.scrollerProxy`
- Lenis destroyed in `onUnmounted`

### GSAP ScrollTrigger Setup

```js
// Pseudocode structure
const tl = gsap.timeline({
  scrollTrigger: {
    trigger: heroRef,
    start: 'top top',
    end: 'bottom bottom',
    scrub: true,
    scroller: window  // or Lenis proxy
  }
})

tl.to(bgImage, { yPercent: -15, ease: 'none' }, 0)
  .to(maskContainer, { scale: 0.92, ease: 'none' }, '25%')
  .to(maskContainer, { scale: 1.0, ease: 'none' }, '40%')
  .fromTo(gridLayer, { opacity: 0 }, { opacity: 1 }, '25%')
  .fromTo(gridLayer, { opacity: 1 }, { opacity: 0 }, '40%')
  // ... dots, slides, hero fade
```

## Edge Cases

- **Mobile**: Disable custom cursor. Mask blocks may need fewer/smaller rects for performance. Reduce parallax intensity.
- **No JS**: Static hero with full image visible (mask, grid, dots are progressive enhancement).
- **Performance**: Use `will-change: transform` on animated layers. Preload background image. Limit SVG rect count to ~50.
- **Resize**: Recalculate ScrollTrigger bounds on window resize (`invalidateOnRefresh: true`).

## Files Changed

| Action | File |
|--------|------|
| Rewrite | `ocean-web/src/views/home/LandingHero.vue` |
| Modify | `ocean-web/src/views/home/HomeView.vue` (Lenis integration) |
| Remove | Existing static styles in LandingHero.vue |

## Open Questions

- Custom cursor: include in this iteration or defer? (deferred by default)
- Exact SVG mask rect layout: tuned visually during implementation
