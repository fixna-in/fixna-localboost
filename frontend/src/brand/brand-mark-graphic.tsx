/**
 * Canonical Fixna mark: white “f” + lime “•” on #163e32 rounded square.
 * Keep `src/app/icon.svg` and `apple-icon.svg` in sync with this graphic.
 */
export function BrandMarkGraphic() {
  return (
    <>
      <rect width="42" height="44" rx="12" fill="#163e32" />
      <text
        x="21"
        y="22"
        dominantBaseline="middle"
        textAnchor="middle"
        fontFamily="system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif"
        fontSize="30"
        fontWeight="700"
        letterSpacing="-0.06em"
      >
        <tspan fill="#ffffff">f</tspan>
        <tspan fill="#c7ed94" dx="-1">•</tspan>
      </text>
    </>
  );
}
