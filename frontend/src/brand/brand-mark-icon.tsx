import { BrandMarkGraphic } from "./brand-mark-graphic";

type BrandMarkIconProps = {
  className?: string;
  width?: number;
  height?: number;
};

/** Header/sidebar logo mark — same artwork as app/icon.svg. */
export function BrandMarkIcon({
  className,
  width = 42,
  height = 44,
}: BrandMarkIconProps) {
  return (
    <svg
      className={className}
      width={width}
      height={height}
      viewBox="0 0 42 44"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <BrandMarkGraphic />
    </svg>
  );
}
